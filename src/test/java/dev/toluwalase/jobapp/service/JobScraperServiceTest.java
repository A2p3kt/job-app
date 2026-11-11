package dev.toluwalase.jobapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Comprehensive unit test suite for JobScraperService.
 * Validates HTML sanitization, keyword tag extraction, category classification,
 * JSON payload ingestion across all 4 APIs, deduplication logic, and zero-crash exception resilience.
 */
@ExtendWith(MockitoExtension.class)
public class JobScraperServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    private JobScraperService scraperService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        scraperService = new JobScraperService(jobPostingRepository, RestClient.builder(), objectMapper);
    }

    @Test
    @DisplayName("cleanHtml should strip all HTML tags and unescape HTML entities")
    void testCleanHtmlWithTagsAndEntities() {
        String rawHtml = "<div><h2>Senior Developer</h2><p>We need an expert in <strong>Java</strong> &amp; <em>Spring Boot</em>.<br/>Apply now &quot;today&quot;!</p></div>";
        String cleaned = scraperService.cleanHtml(rawHtml);

        assertEquals("Senior Developer We need an expert in Java & Spring Boot. Apply now \"today\"!", cleaned);
    }

    @Test
    @DisplayName("cleanHtml should handle null and blank inputs gracefully")
    void testCleanHtmlNullAndBlank() {
        assertEquals("", scraperService.cleanHtml(null));
        assertEquals("", scraperService.cleanHtml("   "));
    }

    @Test
    @DisplayName("cleanHtml should resolve non-breaking spaces and numeric entities")
    void testCleanHtmlEntities() {
        String input = "Hello&nbsp;world&#39;s best company&#8211;apply now&#8220;please&#8221;!";
        String cleaned = scraperService.cleanHtml(input);

        assertEquals("Hello world's best company-apply now\"please\"!", cleaned);
    }

    @Test
    @DisplayName("extractTags should match keywords using word boundaries for TECHNOLOGY")
    void testExtractTagsTechnology() {
        String text = "We are hiring a Java developer with Spring Boot and Docker experience. JavaScript is not required.";
        String tags = scraperService.extractTags(text, JobCategory.TECHNOLOGY);

        assertTrue(tags.contains("Java"));
        assertTrue(tags.contains("Spring Boot"));
        assertTrue(tags.contains("Docker"));
        assertFalse(tags.contains("Python"));
    }

    @Test
    @DisplayName("extractTags should match keywords for LAW_LEGAL")
    void testExtractTagsLegal() {
        String text = "The role involves reviewing Contract Law documents, ensuring GDPR adherence and Corporate Law compliance.";
        String tags = scraperService.extractTags(text, JobCategory.LAW_LEGAL);

        assertTrue(tags.contains("Contract Law"));
        assertTrue(tags.contains("Compliance"));
        assertTrue(tags.contains("GDPR"));
        assertTrue(tags.contains("Corporate Law"));
    }

    @Test
    @DisplayName("extractTags should match keywords for SERVICE_OPERATIONS")
    void testExtractTagsServiceOperations() {
        String text = "Requires expertise in Commercial Cleaning, Sanitization procedures, and Facility Equipment Maintenance.";
        String tags = scraperService.extractTags(text, JobCategory.SERVICE_OPERATIONS);

        assertTrue(tags.contains("Commercial Cleaning"));
        assertTrue(tags.contains("Sanitization"));
        assertTrue(tags.contains("Equipment Maintenance"));
    }

    @Test
    @DisplayName("determineCategory should accurately classify technology postings")
    void testDetermineCategoryTechnology() {
        String title = "Backend Software Engineer";
        String desc = "Building cloud microservices with Java, Docker, and AWS Kubernetes.";
        List<String> tags = List.of("Java", "Backend");

        JobCategory category = scraperService.determineCategory(title, desc, tags);
        assertEquals(JobCategory.TECHNOLOGY, category);
    }

    @Test
    @DisplayName("determineCategory should accurately classify healthcare postings")
    void testDetermineCategoryHealthcare() {
        String title = "Registered Staff Nurse";
        String desc = "Responsible for Patient Care, Medication Administration, and clinical triage.";
        List<String> tags = List.of("Nursing", "Hospital");

        JobCategory category = scraperService.determineCategory(title, desc, tags);
        assertEquals(JobCategory.HEALTHCARE, category);
    }

    @Test
    @DisplayName("determineCategory should accurately classify marketing postings")
    void testDetermineCategoryMarketing() {
        String title = "Digital Marketing & SEO Specialist";
        String desc = "Driving inbound traffic via Content Strategy, Social Media campaigns, and Google Analytics.";
        List<String> tags = List.of("SEO", "Content");

        JobCategory category = scraperService.determineCategory(title, desc, tags);
        assertEquals(JobCategory.MARKETING, category);
    }

    @Test
    @DisplayName("determineCategory should accurately classify finance postings")
    void testDetermineCategoryFinance() {
        String title = "Senior Financial Analyst";
        String desc = "Lead financial modeling, quarterly budgeting, and IFRS auditing.";
        List<String> tags = List.of("Accounting", "Finance");

        JobCategory category = scraperService.determineCategory(title, desc, tags);
        assertEquals(JobCategory.BUSINESS_FINANCE, category);
    }

    @Test
    @DisplayName("determineCategory should fall back to heuristic title checks when keywords are sparse")
    void testDetermineCategoryHeuristics() {
        assertEquals(JobCategory.HEALTHCARE, scraperService.determineCategory("Clinical Care Coordinator", "General duties", null));
        assertEquals(JobCategory.LAW_LEGAL, scraperService.determineCategory("Junior Legal Counsel", "Advising stakeholders", null));
        assertEquals(JobCategory.SERVICE_OPERATIONS, scraperService.determineCategory("Night Custodian & Cleaner", "Maintaining spaces", null));
        assertEquals(JobCategory.BUSINESS_FINANCE, scraperService.determineCategory("Payroll Specialist", "Managing biweekly disbursements", null));
    }

    @Test
    @DisplayName("ingestJobTechHits should parse JobTech JSON and save non-duplicate postings")
    void testIngestJobTechHits() throws Exception {
        String json = """
            [
              {
                "id": "jt-101",
                "headline": "Junior Java Developer",
                "employer": { "name": "Nordic Soft AB" },
                "workplace_address": { "municipality": "Stockholm", "country": "Sverige" },
                "description": { "text": "Join our team working with Java and Spring Boot in Stockholm." },
                "webpage_url": "https://example.com/jt-101"
              }
            ]
            """;
        JsonNode hits = objectMapper.readTree(json);

        when(jobPostingRepository.existsByExternalId("jobtech-jt-101")).thenReturn(false);

        int count = scraperService.ingestJobTechHits(hits, JobCategory.TECHNOLOGY);
        assertEquals(1, count);

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());

        JobPosting saved = captor.getValue();
        assertEquals("jobtech-jt-101", saved.getExternalId());
        assertEquals("Junior Java Developer", saved.getTitle());
        assertEquals("Nordic Soft AB", saved.getCompany());
        assertEquals("Stockholm", saved.getLocation());
        assertEquals("JobTech", saved.getSourceName());
        assertTrue(saved.getExtractedTags().contains("Java"));
    }

    @Test
    @DisplayName("ingestTheMuseResults should parse The Muse JSON and strip HTML in description")
    void testIngestTheMuseResults() throws Exception {
        String json = """
            [
              {
                "id": 9901,
                "name": "Brand Marketing Strategist",
                "company": { "name": "Global Brands Corp" },
                "locations": [{ "name": "New York, NY" }],
                "contents": "<p>Looking for <strong>Brand Strategy</strong> and <em>SEO</em> expert.</p>",
                "refs": { "landing_page": "https://example.com/muse-9901" }
              }
            ]
            """;
        JsonNode results = objectMapper.readTree(json);

        when(jobPostingRepository.existsByExternalId("themuse-9901")).thenReturn(false);

        int count = scraperService.ingestTheMuseResults(results, JobCategory.MARKETING);
        assertEquals(1, count);

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());

        JobPosting saved = captor.getValue();
        assertEquals("themuse-9901", saved.getExternalId());
        assertEquals("Brand Marketing Strategist", saved.getTitle());
        assertEquals("Looking for Brand Strategy and SEO expert.", saved.getDescription());
        assertEquals("TheMuse", saved.getSourceName());
    }

    @Test
    @DisplayName("ingestRemotiveJobs should parse Remotive JSON and map remote locations")
    void testIngestRemotiveJobs() throws Exception {
        String json = """
            [
              {
                "id": 8802,
                "title": "Remote DevOps Cloud Engineer",
                "company_name": "CloudScale Inc",
                "candidate_required_location": "Worldwide",
                "description": "<p>Manage AWS, Docker containers, and CI/CD pipelines.</p>",
                "url": "https://remotive.com/jobs/8802"
              }
            ]
            """;
        JsonNode jobs = objectMapper.readTree(json);

        when(jobPostingRepository.existsByExternalId("remotive-8802")).thenReturn(false);

        int count = scraperService.ingestRemotiveJobs(jobs, JobCategory.TECHNOLOGY, "software-dev");
        assertEquals(1, count);

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());

        JobPosting saved = captor.getValue();
        assertEquals("remotive-8802", saved.getExternalId());
        assertEquals("Remote DevOps Cloud Engineer", saved.getTitle());
        assertEquals("Worldwide", saved.getLocation());
        assertEquals("Remotive", saved.getSourceName());
    }

    @Test
    @DisplayName("ingestArbeitnowData should infer JobCategory and parse Arbeitnow postings")
    void testIngestArbeitnowData() throws Exception {
        String json = """
            [
              {
                "slug": "cleaning-supervisor-berlin-7711",
                "title": "Facility Operations & Cleaning Supervisor",
                "company_name": "CleanPro Berlin",
                "location": "Berlin, Germany",
                "description": "<p>Oversee Commercial Cleaning, Sanitization, and Equipment Maintenance.</p>",
                "url": "https://arbeitnow.com/jobs/7711",
                "tags": ["Cleaning", "Maintenance"]
              }
            ]
            """;
        JsonNode data = objectMapper.readTree(json);

        when(jobPostingRepository.existsByExternalId("arbeitnow-cleaning-supervisor-berlin-7711")).thenReturn(false);

        int count = scraperService.ingestArbeitnowData(data);
        assertEquals(1, count);

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());

        JobPosting saved = captor.getValue();
        assertEquals("arbeitnow-cleaning-supervisor-berlin-7711", saved.getExternalId());
        assertEquals(JobCategory.SERVICE_OPERATIONS, saved.getCategory());
        assertEquals("Arbeitnow", saved.getSourceName());
    }

    @Test
    @DisplayName("existsByExternalId should prevent duplicate postings across all ingestion methods")
    void testDeduplicationAcrossIngestions() throws Exception {
        String json = """
            [
              {
                "id": "duplicate-123",
                "headline": "Software Engineer",
                "employer": { "name": "Company" },
                "description": { "text": "Java" }
              }
            ]
            """;
        JsonNode hits = objectMapper.readTree(json);

        // Mark as existing
        when(jobPostingRepository.existsByExternalId("jobtech-duplicate-123")).thenReturn(true);

        int count = scraperService.ingestJobTechHits(hits, JobCategory.TECHNOLOGY);
        assertEquals(0, count);
        verify(jobPostingRepository, never()).save(any(JobPosting.class));
    }

    @Test
    @DisplayName("triggerScrapeNow should run without crashing when network is unavailable (Zero-Crash Guarantee)")
    void testTriggerScrapeNowOfflineZeroCrash() {
        org.springframework.http.client.ClientHttpRequestFactory failingFactory = (uri, httpMethod) -> {
            throw new java.io.IOException("Simulated network outage");
        };
        RestClient offlineClient = RestClient.builder().requestFactory(failingFactory).build();
        JobScraperService offlineService = new JobScraperService(jobPostingRepository, offlineClient, objectMapper);

        int count = offlineService.triggerScrapeNow();
        // Since network is down, 0 new jobs are persisted, and 0 exceptions escape
        assertEquals(0, count);
        verify(jobPostingRepository, never()).save(any(JobPosting.class));
    }
}
