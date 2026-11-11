package dev.toluwalase.jobapp.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Multi-industry job scraper and ingestion service.
 * Periodically retrieves external market postings from public zero-auth APIs:
 * <ul>
 *   <li>JobTech OpenAPI (Nordics / Multi-Industry)</li>
 *   <li>The Muse Public API (Global / Multi-Industry)</li>
 *   <li>Remotive API (Global Remote / Tech, Marketing, Business)</li>
 *   <li>Arbeitnow API (European Job Board / Multi-Industry)</li>
 * </ul>
 * Sanitizes HTML content, normalizes whitespace, tags listings against domain keyword dictionaries,
 * and enforces deduplication based on external identifiers.
 */
@Service
public class JobScraperService {

    private static final Logger log = LoggerFactory.getLogger(JobScraperService.class);

    private final JobPostingRepository jobPostingRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Primary constructor with Spring dependency injection.
     * Configures the RestClient with realistic browser headers and network timeouts.
     *
     * @param jobPostingRepository Repository for persisting job postings
     * @param restClientBuilder    Spring-managed RestClient builder
     * @param objectMapper         Jackson object mapper for JSON processing
     */
    @Autowired
    public JobScraperService(JobPostingRepository jobPostingRepository,
                             @Autowired(required = false) RestClient.Builder restClientBuilder,
                             @Autowired(required = false) ObjectMapper objectMapper) {
        this.jobPostingRepository = jobPostingRepository;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(6));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient.Builder builder = (restClientBuilder != null) ? restClientBuilder : RestClient.builder();
        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 JobApp/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Overloaded constructor for unit testing with a mock or custom RestClient.
     *
     * @param jobPostingRepository Repository for persisting job postings
     * @param restClient           Custom RestClient instance
     * @param objectMapper         Jackson object mapper
     */
    public JobScraperService(JobPostingRepository jobPostingRepository,
                             RestClient restClient,
                             ObjectMapper objectMapper) {
        this.jobPostingRepository = jobPostingRepository;
        this.restClient = restClient;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    /**
     * Recurring hourly ingestion job.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledScrape() {
        log.info("Hourly job scraper triggered via cron schedule.");
        int persisted = triggerScrapeNow();
        log.info("Hourly job scraper completed. Persisted {} new job postings.", persisted);
    }

    /**
     * Manually triggers multi-source job ingestion across all configured endpoints.
     *
     * @return Total count of new JobPosting records successfully saved
     */
    public int triggerScrapeNow() {
        log.info("Starting manual multi-source job ingestion run...");
        int totalSaved = 0;

        totalSaved += scrapeJobTech();
        totalSaved += scrapeTheMuse();
        totalSaved += scrapeRemotive();
        totalSaved += scrapeArbeitnow();

        log.info("Multi-source job ingestion finished. Total new listings inserted: {}", totalSaved);
        return totalSaved;
    }

    /**
     * Scrapes opportunities across the Nordic region using the JobTech OpenAPI.
     *
     * @return Count of newly persisted JobTech postings
     */
    public int scrapeJobTech() {
        int count = 0;
        log.info("Querying JobTech OpenAPI (Nordics)...");

        for (JobCategory category : JobCategory.values()) {
            try {
                String encodedQuery = URLEncoder.encode(category.getSearchQuery(), StandardCharsets.UTF_8);
                String url = "https://jobsearch.api.jobtechdev.se/search?q=" + encodedQuery + "&limit=12";

                String responseBody = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode hits = root.path("hits");
                count += ingestJobTechHits(hits, category);
            } catch (Exception ex) {
                log.warn("JobTech scraper encountered an error for category {}: {}", category.name(), ex.getMessage());
            }
        }

        log.info("JobTech scraper concluded. Added {} new records.", count);
        return count;
    }

    /**
     * Parses and persists an array of JobTech hit JSON nodes.
     *
     * @param hits     JSON array node containing JobTech hits
     * @param category Inferred or target job category
     * @return Count of newly persisted postings
     */
    public int ingestJobTechHits(JsonNode hits, JobCategory category) {
        if (hits == null || !hits.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode hit : hits) {
            try {
                String rawId = hit.path("id").asText();
                if (rawId == null || rawId.isBlank()) {
                    continue;
                }
                String externalId = "jobtech-" + rawId.trim();
                if (jobPostingRepository.existsByExternalId(externalId)) {
                    continue;
                }

                String title = hit.path("headline").asText("").trim();
                if (title.isBlank()) {
                    title = "Professional Opportunity";
                }

                String company = hit.path("employer").path("name").asText("").trim();
                if (company.isBlank()) {
                    company = "Confidential Employer";
                }

                String municipality = hit.path("workplace_address").path("municipality").asText("").trim();
                String city = hit.path("workplace_address").path("city").asText("").trim();
                String country = hit.path("workplace_address").path("country").asText("").trim();
                String location = !municipality.isBlank() ? municipality : (!city.isBlank() ? city : (!country.isBlank() ? country : "Nordics"));

                String rawDesc = hit.path("description").path("text").asText("");
                String cleanDesc = cleanHtml(rawDesc);
                String sourceUrl = hit.path("webpage_url").asText("");

                String tags = extractTags(cleanDesc + " " + title, category);

                JobPosting posting = new JobPosting(
                        externalId,
                        category,
                        title,
                        company,
                        location,
                        cleanDesc,
                        tags,
                        sourceUrl,
                        "JobTech",
                        LocalDateTime.now()
                );

                jobPostingRepository.save(posting);
                count++;
            } catch (Exception ex) {
                log.warn("Failed to parse single JobTech record: {}", ex.getMessage());
            }
        }
        return count;
    }

    /**
     * Scrapes opportunities using The Muse Public API.
     *
     * @return Count of newly persisted The Muse postings
     */
    public int scrapeTheMuse() {
        int count = 0;
        log.info("Querying The Muse Public API...");

        for (JobCategory category : JobCategory.values()) {
            String museCat = category.getTheMuseCategory();
            if (museCat == null || museCat.isBlank()) {
                continue;
            }

            try {
                String encodedCategory = URLEncoder.encode(museCat, StandardCharsets.UTF_8);
                String url = "https://www.themuse.com/api/public/jobs?category=" + encodedCategory + "&page=1";

                String responseBody = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode results = root.path("results");
                count += ingestTheMuseResults(results, category);
            } catch (Exception ex) {
                log.warn("The Muse scraper encountered an error for category {}: {}", category.name(), ex.getMessage());
            }
        }

        log.info("The Muse scraper concluded. Added {} new records.", count);
        return count;
    }

    /**
     * Parses and persists an array of The Muse job JSON nodes.
     *
     * @param results  JSON array node containing The Muse results
     * @param category Inferred or target job category
     * @return Count of newly persisted postings
     */
    public int ingestTheMuseResults(JsonNode results, JobCategory category) {
        if (results == null || !results.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode node : results) {
            try {
                String rawId = node.path("id").asText();
                if (rawId == null || rawId.isBlank()) {
                    continue;
                }
                String externalId = "themuse-" + rawId.trim();
                if (jobPostingRepository.existsByExternalId(externalId)) {
                    continue;
                }

                String title = node.path("name").asText("").trim();
                if (title.isBlank()) {
                    title = "Professional Opportunity";
                }

                String company = node.path("company").path("name").asText("").trim();
                if (company.isBlank()) {
                    company = "Confidential Employer";
                }

                String location = node.path("locations").path(0).path("name").asText("Flexible / Remote").trim();
                if (location.isBlank()) {
                    location = "Flexible / Remote";
                }

                String rawDesc = node.path("contents").asText("");
                String cleanDesc = cleanHtml(rawDesc);
                String sourceUrl = node.path("refs").path("landing_page").asText("");

                String tags = extractTags(cleanDesc + " " + title, category);

                JobPosting posting = new JobPosting(
                        externalId,
                        category,
                        title,
                        company,
                        location,
                        cleanDesc,
                        tags,
                        sourceUrl,
                        "TheMuse",
                        LocalDateTime.now()
                );

                jobPostingRepository.save(posting);
                count++;
            } catch (Exception ex) {
                log.warn("Failed to parse single The Muse record: {}", ex.getMessage());
            }
        }
        return count;
    }

    /**
     * Scrapes remote job openings from the Remotive API.
     *
     * @return Count of newly persisted Remotive postings
     */
    public int scrapeRemotive() {
        int count = 0;
        log.info("Querying Remotive API...");
        Set<String> processedSlugs = new HashSet<>();

        for (JobCategory category : JobCategory.values()) {
            String slug = category.getRemotiveCategory();
            if (slug == null || slug.isBlank() || processedSlugs.contains(slug)) {
                continue;
            }
            processedSlugs.add(slug);

            try {
                String url = "https://remotive.com/api/remote-jobs?category=" + slug + "&limit=10";

                String responseBody = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode jobs = root.path("jobs");
                count += ingestRemotiveJobs(jobs, category, slug);
            } catch (Exception ex) {
                log.warn("Remotive scraper encountered an error for slug {}: {}", slug, ex.getMessage());
            }
        }

        log.info("Remotive scraper concluded. Added {} new records.", count);
        return count;
    }

    /**
     * Parses and persists an array of Remotive job JSON nodes.
     *
     * @param jobs         JSON array node containing Remotive jobs
     * @param category     Fallback job category
     * @param categorySlug Category query slug
     * @return Count of newly persisted postings
     */
    public int ingestRemotiveJobs(JsonNode jobs, JobCategory category, String categorySlug) {
        if (jobs == null || !jobs.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode node : jobs) {
            try {
                String rawId = node.path("id").asText();
                if (rawId == null || rawId.isBlank()) {
                    continue;
                }
                String externalId = "remotive-" + rawId.trim();
                if (jobPostingRepository.existsByExternalId(externalId)) {
                    continue;
                }

                String title = node.path("title").asText("").trim();
                if (title.isBlank()) {
                    title = "Remote Position";
                }

                String company = node.path("company_name").asText("").trim();
                if (company.isBlank()) {
                    company = "Confidential Employer";
                }

                String location = node.path("candidate_required_location").asText("Worldwide Remote").trim();
                if (location.isBlank()) {
                    location = "Worldwide Remote";
                }

                String rawDesc = node.path("description").asText("");
                String cleanDesc = cleanHtml(rawDesc);
                String sourceUrl = node.path("url").asText("");

                JobCategory targetCategory = category;
                if ("all-others".equals(categorySlug)) {
                    targetCategory = determineCategory(title, cleanDesc, null);
                }

                String tags = extractTags(cleanDesc + " " + title, targetCategory);

                JobPosting posting = new JobPosting(
                        externalId,
                        targetCategory,
                        title,
                        company,
                        location,
                        cleanDesc,
                        tags,
                        sourceUrl,
                        "Remotive",
                        LocalDateTime.now()
                );

                jobPostingRepository.save(posting);
                count++;
            } catch (Exception ex) {
                log.warn("Failed to parse single Remotive record: {}", ex.getMessage());
            }
        }
        return count;
    }

    /**
     * Scrapes European and global listings from the Arbeitnow API.
     *
     * @return Count of newly persisted Arbeitnow postings
     */
    public int scrapeArbeitnow() {
        int count = 0;
        log.info("Querying Arbeitnow API...");

        try {
            String url = "https://www.arbeitnow.com/api/job-board-api";

            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return 0;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            count += ingestArbeitnowData(data);
        } catch (Exception ex) {
            log.warn("Arbeitnow scraper encountered an error: {}", ex.getMessage());
        }

        log.info("Arbeitnow scraper concluded. Added {} new records.", count);
        return count;
    }

    /**
     * Parses and persists an array of Arbeitnow job JSON nodes.
     *
     * @param data JSON array node containing Arbeitnow jobs
     * @return Count of newly persisted postings
     */
    public int ingestArbeitnowData(JsonNode data) {
        if (data == null || !data.isArray()) {
            return 0;
        }

        int count = 0;
        int limit = Math.min(data.size(), 30);
        for (int i = 0; i < limit; i++) {
            JsonNode node = data.get(i);
            try {
                String slug = node.path("slug").asText();
                if (slug == null || slug.isBlank()) {
                    continue;
                }
                String externalId = "arbeitnow-" + slug.trim();
                if (jobPostingRepository.existsByExternalId(externalId)) {
                    continue;
                }

                String title = node.path("title").asText("").trim();
                if (title.isBlank()) {
                    title = "Professional Opportunity";
                }

                String company = node.path("company_name").asText("").trim();
                if (company.isBlank()) {
                    company = "Confidential Employer";
                }

                String location = node.path("location").asText("Europe / Remote").trim();
                if (location.isBlank()) {
                    location = "Europe / Remote";
                }

                String rawDesc = node.path("description").asText("");
                String cleanDesc = cleanHtml(rawDesc);
                String sourceUrl = node.path("url").asText("");

                List<String> rawTags = new ArrayList<>();
                JsonNode tagsNode = node.path("tags");
                if (tagsNode.isArray()) {
                    for (JsonNode t : tagsNode) {
                        String textTag = t.asText("").trim();
                        if (!textTag.isBlank()) {
                            rawTags.add(textTag);
                        }
                    }
                }

                JobCategory targetCategory = determineCategory(title, cleanDesc, rawTags);
                String tags = extractTags(cleanDesc + " " + title + " " + String.join(" ", rawTags), targetCategory);

                JobPosting posting = new JobPosting(
                        externalId,
                        targetCategory,
                        title,
                        company,
                        location,
                        cleanDesc,
                        tags,
                        sourceUrl,
                        "Arbeitnow",
                        LocalDateTime.now()
                );

                jobPostingRepository.save(posting);
                count++;
            } catch (Exception ex) {
                log.warn("Failed to parse single Arbeitnow record: {}", ex.getMessage());
            }
        }
        return count;
    }

    /**
     * Strips HTML markup, resolves common HTML entities, and normalizes redundant whitespace.
     *
     * @param html Raw HTML content
     * @return Cleaned plain-text string
     */
    public String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // Replace common block tags with spaces
        String text = html.replaceAll("(?i)<(br|p|div|li|tr|h[1-6])[^>]*>", " ");
        // Strip remaining HTML tags
        text = text.replaceAll("<[^>]*>", " ");

        // Unescape standard HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&bull;", " • ")
                .replace("&#8211;", "-")
                .replace("&#8212;", "--")
                .replace("&#8216;", "'")
                .replace("&#8217;", "'")
                .replace("&#8220;", "\"")
                .replace("&#8221;", "\"");

        // Strip numeric entity codes
        text = text.replaceAll("&#[0-9]+;", " ");
        text = text.replaceAll("&#x[0-9a-fA-F]+;", " ");

        // Fix punctuation spacing: e.g. "word ." -> "word."
        text = text.replaceAll("\\s+([.,!?:;])", "$1");

        // Normalize multiple spaces into single space
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Cross-references input text against a JobCategory's curated keyword dictionary.
     * Uses regex word boundaries to avoid substring false positives.
     *
     * @param text     Combined title, description, and metadata text
     * @param category Target career specialization category
     * @return Comma-separated list of matched keywords
     */
    public String extractTags(String text, JobCategory category) {
        if (text == null || text.isBlank() || category == null) {
            return "";
        }

        String lower = text.toLowerCase();
        List<String> matched = new ArrayList<>();

        for (String kw : category.getKeywords()) {
            Pattern pattern = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(kw) + "(?=[^a-zA-Z0-9]|$)");
            if (pattern.matcher(lower).find()) {
                matched.add(kw);
            }
        }

        return String.join(", ", matched);
    }

    /**
     * Automatically infers the most appropriate JobCategory for an unclassified posting
     * by scoring title, tags, and description against all category keyword dictionaries.
     *
     * @param title       Job title
     * @param description Sanitized description text
     * @param extraTags   Optional raw tag labels provided by the portal
     * @return Best matching JobCategory
     */
    public JobCategory determineCategory(String title, String description, List<String> extraTags) {
        String lowerTitle = (title != null) ? title.toLowerCase() : "";
        String tagsJoined = (extraTags != null) ? String.join(" ", extraTags).toLowerCase() : "";
        String lowerDesc = (description != null) ? description.toLowerCase() : "";

        JobCategory bestCategory = JobCategory.TECHNOLOGY;
        int highestScore = -1;

        for (JobCategory category : JobCategory.values()) {
            int score = 0;
            for (String kw : category.getKeywords()) {
                Pattern pattern = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(kw) + "(?=[^a-zA-Z0-9]|$)");
                if (!lowerTitle.isBlank() && pattern.matcher(lowerTitle).find()) {
                    score += 5;
                }
                if (!tagsJoined.isBlank() && pattern.matcher(tagsJoined).find()) {
                    score += 3;
                }
                if (!lowerDesc.isBlank() && pattern.matcher(lowerDesc).find()) {
                    score += 1;
                }
            }

            if (score > highestScore) {
                highestScore = score;
                bestCategory = category;
            }
        }

        if (highestScore <= 0) {
            // Heuristic keyword fallback on job title
            if (lowerTitle.contains("nurse") || lowerTitle.contains("care") || lowerTitle.contains("clinic") || lowerTitle.contains("health")) {
                return JobCategory.HEALTHCARE;
            }
            if (lowerTitle.contains("legal") || lowerTitle.contains("law") || lowerTitle.contains("compliance") || lowerTitle.contains("attorney")) {
                return JobCategory.LAW_LEGAL;
            }
            if (lowerTitle.contains("marketing") || lowerTitle.contains("seo") || lowerTitle.contains("brand") || lowerTitle.contains("content")) {
                return JobCategory.MARKETING;
            }
            if (lowerTitle.contains("clean") || lowerTitle.contains("facility") || lowerTitle.contains("maintenance") || lowerTitle.contains("custodian")) {
                return JobCategory.SERVICE_OPERATIONS;
            }
            if (lowerTitle.contains("finance") || lowerTitle.contains("account") || lowerTitle.contains("budget") || lowerTitle.contains("audit")) {
                return JobCategory.BUSINESS_FINANCE;
            }
        }

        return bestCategory;
    }
}
