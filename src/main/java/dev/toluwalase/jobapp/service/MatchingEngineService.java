package dev.toluwalase.jobapp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Domain-aware matching engine service implementing a two-tier scoring algorithm (0–100 scale).
 * <p>
 * Evaluates candidate fit across two distinct tiers:
 * <ul>
 *   <li><b>Tier 1: Preference Alignment (40% Weight):</b> Location and preferred role tag overlap.</li>
 *   <li><b>Tier 2: Profile Evidence Overlap (60% Weight):</b> Candidate skills list, career summary,
 *       and structured ProfileItem evidence (experiences, education, certifications, projects)
 *       matched against job requirements and extracted tags.</li>
 * </ul>
 * Also incorporates specialization category prioritization with transparent rationale generation.
 */
@Service
public class MatchingEngineService {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineService.class);

    private final JobPostingRepository jobPostingRepository;

    /**
     * Explicit constructor injection without Lombok.
     *
     * @param jobPostingRepository Repository for accessing persisted job postings
     */
    public MatchingEngineService(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Scores and ranks all persisted job postings against a candidate's MasterProfile.
     * Sorts descending by match score.
     *
     * @param profile Candidate profile
     * @return Ranked list of JobMatchDto records
     */
    public List<JobMatchDto> rankJobsForProfile(MasterProfile profile) {
        if (profile == null) {
            return Collections.emptyList();
        }
        List<JobPosting> allJobs = jobPostingRepository.findByOrderByScrapedAtDesc();
        return rankJobsForProfile(profile, allJobs);
    }

    /**
     * Scores and ranks a filtered list of jobs matching a specific industry category.
     *
     * @param profile  Candidate profile
     * @param category Target industry category
     * @return Ranked list of JobMatchDto records
     */
    public List<JobMatchDto> rankJobsForCategory(MasterProfile profile, JobCategory category) {
        if (profile == null) {
            return Collections.emptyList();
        }
        List<JobPosting> categoryJobs = (category != null)
                ? jobPostingRepository.findByCategoryOrderByScrapedAtDesc(category)
                : jobPostingRepository.findByOrderByScrapedAtDesc();
        return rankJobsForProfile(profile, categoryJobs);
    }

    /**
     * Evaluates a provided collection of JobPosting records against a MasterProfile.
     *
     * @param profile Candidate profile
     * @param jobs    List of JobPosting entities to evaluate
     * @return Ranked list of JobMatchDto records sorted from highest match score to lowest
     */
    public List<JobMatchDto> rankJobsForProfile(MasterProfile profile, List<JobPosting> jobs) {
        if (profile == null || jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        List<JobMatchDto> matches = new ArrayList<>();
        for (JobPosting job : jobs) {
            matches.add(scoreJob(profile, job));
        }

        Collections.sort(matches);
        return matches;
    }

    /**
     * Calculates the two-tier match score (0–100) for a single job against the candidate's profile.
     *
     * @param profile Candidate MasterProfile
     * @param job     Target JobPosting
     * @return Complete JobMatchDto containing subscores and rationale list
     */
    public JobMatchDto scoreJob(MasterProfile profile, JobPosting job) {
        if (profile == null || job == null) {
            return new JobMatchDto(job, 0, 0, 0, Collections.emptyList());
        }

        Set<String> rationale = new LinkedHashSet<>();

        // -------------------------------------------------------------
        // Specialization Alignment & Status
        // -------------------------------------------------------------
        boolean isSpecializationMatch = (job.getCategory() == profile.getSpecialization());
        if (isSpecializationMatch) {
            rationale.add("Specialization Match: " + job.getCategory().getDisplayName());
        } else {
            rationale.add("Cross-Industry Opportunity (" + job.getCategory().getDisplayName() + ")");
        }

        // -------------------------------------------------------------
        // TIER 1: Candidate Preference Alignment (Max 40 points)
        // -------------------------------------------------------------
        int locationScore = scoreLocationPreference(profile, job, rationale);
        int tagPreferenceScore = scoreTagPreference(profile, job, rationale);
        int tier1Score = Math.min(40, locationScore + tagPreferenceScore);

        // -------------------------------------------------------------
        // TIER 2: Profile Evidence Overlap (Max 60 points)
        // -------------------------------------------------------------
        int skillsScore = scoreSkillsEvidence(profile, job, rationale);
        int backgroundScore = scoreBackgroundEvidence(profile, job, rationale);
        int tier2Score = Math.min(60, skillsScore + backgroundScore);

        int rawCompositeScore = tier1Score + tier2Score;

        int finalScore;
        if (isSpecializationMatch) {
            finalScore = rawCompositeScore;
        } else {
            // Apply 40% penalty for jobs outside the candidate's active specialization vertical
            finalScore = (int) Math.round(rawCompositeScore * 0.60);
        }

        finalScore = Math.max(0, Math.min(100, finalScore));

        List<String> trimmedRationale = rationale.stream().limit(10).collect(Collectors.toList());

        return new JobMatchDto(job, finalScore, tier1Score, tier2Score, trimmedRationale);
    }

    /**
     * Evaluates location alignment between candidate preferences and job listing (0–20 points).
     */
    private int scoreLocationPreference(MasterProfile profile, JobPosting job, Set<String> rationale) {
        String jobLoc = (job.getLocation() != null) ? job.getLocation().toLowerCase() : "";
        String preferredLocs = profile.getPreferredLocations();

        if (preferredLocs == null || preferredLocs.isBlank()) {
            // Neutral default score if candidate has no explicit location restriction
            return 12;
        }

        List<String> candidatesLocs = Arrays.stream(preferredLocs.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (candidatesLocs.isEmpty()) {
            return 12;
        }

        for (String preferred : candidatesLocs) {
            String lowerPreferred = preferred.toLowerCase();
            // Check remote compatibility
            if (lowerPreferred.contains("remote") && (jobLoc.contains("remote") || jobLoc.contains("hybrid") || jobLoc.contains("worldwide"))) {
                rationale.add("Location Match: Remote / Hybrid");
                return 20;
            }
            // Check municipality or city substring
            if (!lowerPreferred.isBlank() && jobLoc.contains(lowerPreferred)) {
                rationale.add("Location Match: " + preferred);
                return 20;
            }
        }

        return 0;
    }

    /**
     * Evaluates candidate preferred roles and domain tags against job listing (0–20 points).
     */
    private int scoreTagPreference(MasterProfile profile, JobPosting job, Set<String> rationale) {
        String preferredTags = profile.getPreferredTags();
        if (preferredTags == null || preferredTags.isBlank()) {
            return 12;
        }

        List<String> tags = Arrays.stream(preferredTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            return 12;
        }

        String jobContent = ((job.getTitle() != null ? job.getTitle() : "") + " "
                + (job.getExtractedTags() != null ? job.getExtractedTags() : "") + " "
                + (job.getDescription() != null ? job.getDescription() : "")).toLowerCase();

        int matchedCount = 0;
        for (String tag : tags) {
            Pattern pattern = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(tag) + "(?=[^a-zA-Z0-9]|$)");
            if (pattern.matcher(jobContent).find()) {
                matchedCount++;
                rationale.add("Target Role Tag: " + tag);
            }
        }

        double ratio = (double) matchedCount / tags.size();
        return (int) Math.round(ratio * 20.0);
    }

    /**
     * Evaluates direct skill overlap between candidate skillsList and job extractedTags (0–40 points).
     */
    private int scoreSkillsEvidence(MasterProfile profile, JobPosting job, Set<String> rationale) {
        String skillsList = profile.getSkillsList();
        if (skillsList == null || skillsList.isBlank()) {
            return 0;
        }

        List<String> candidateSkills = Arrays.stream(skillsList.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (candidateSkills.isEmpty()) {
            return 0;
        }

        String extractedTags = job.getExtractedTags();
        String jobFullText = ((job.getTitle() != null ? job.getTitle() : "") + " "
                + (job.getDescription() != null ? job.getDescription() : "")).toLowerCase();

        if (extractedTags != null && !extractedTags.isBlank()) {
            List<String> jobTags = Arrays.stream(extractedTags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

            if (!jobTags.isEmpty()) {
                int matchedTags = 0;
                for (String jobTag : jobTags) {
                    boolean found = false;
                    for (String skill : candidateSkills) {
                        if (skill.equalsIgnoreCase(jobTag)) {
                            matchedTags++;
                            rationale.add("Skill Match: " + jobTag);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Check if candidate skill is a word in the job tag or vice versa
                        for (String skill : candidateSkills) {
                            if (jobTag.toLowerCase().contains(skill.toLowerCase()) || skill.toLowerCase().contains(jobTag.toLowerCase())) {
                                matchedTags++;
                                rationale.add("Skill Match: " + jobTag);
                                break;
                            }
                        }
                    }
                }

                double ratio = (double) matchedTags / jobTags.size();
                int score = (int) Math.round(ratio * 35.0);

                // Check for additional candidate skills mentioned in description
                for (String skill : candidateSkills) {
                    if (rationale.stream().noneMatch(r -> r.equalsIgnoreCase("Skill Match: " + skill))) {
                        Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(skill) + "(?=[^a-zA-Z0-9]|$)");
                        if (p.matcher(jobFullText).find()) {
                            score += 3;
                            rationale.add("Skill in Description: " + skill);
                        }
                    }
                }

                return Math.min(40, score);
            }
        }

        // Fallback when job has no extractedTags: direct regex matching of candidate skills in description/title
        int score = 0;
        for (String skill : candidateSkills) {
            Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(skill) + "(?=[^a-zA-Z0-9]|$)");
            if (p.matcher(jobFullText).find()) {
                score += 8;
                rationale.add("Skill Match: " + skill);
            }
        }

        return Math.min(40, score);
    }

    /**
     * Evaluates concrete proof in ProfileItem entities (Experience, Education, Certification, Project)
     * and candidate summary text (0–20 points).
     */
    private int scoreBackgroundEvidence(MasterProfile profile, JobPosting job, Set<String> rationale) {
        int score = 0;
        String jobTitle = (job.getTitle() != null) ? job.getTitle().toLowerCase() : "";
        String jobTags = (job.getExtractedTags() != null) ? job.getExtractedTags().toLowerCase() : "";
        String jobDesc = (job.getDescription() != null) ? job.getDescription().toLowerCase() : "";

        // 1. Evaluate ProfileItem evidence
        List<ProfileItem> items = profile.getItems();
        if (items != null) {
            for (ProfileItem item : items) {
                String itemTitle = (item.getTitle() != null) ? item.getTitle().toLowerCase() : "";
                String itemDesc = (item.getDescription() != null) ? item.getDescription().toLowerCase() : "";

                if (item.getType() == ProfileItemType.EXPERIENCE) {
                    // Check if experience title or description matches job title or tags
                    boolean matches = false;
                    for (String word : jobTitle.split("\\s+")) {
                        if (word.length() > 3 && itemTitle.contains(word)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches && !jobTags.isBlank()) {
                        for (String tag : jobTags.split(",")) {
                            String t = tag.trim();
                            if (!t.isBlank() && (itemTitle.contains(t) || itemDesc.contains(t))) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        score += 5;
                        rationale.add("Experience: " + item.getTitle());
                    }
                } else if (item.getType() == ProfileItemType.CERTIFICATION) {
                    boolean certMatches = false;
                    if (!jobTags.isBlank()) {
                        for (String tag : jobTags.split(",")) {
                            String t = tag.trim();
                            if (!t.isBlank() && itemTitle.contains(t)) {
                                certMatches = true;
                                break;
                            }
                        }
                    }
                    if (certMatches || jobDesc.contains(itemTitle) || jobTitle.contains(itemTitle)) {
                        score += 5;
                        rationale.add("Certification: " + item.getTitle());
                    }
                } else if (item.getType() == ProfileItemType.EDUCATION) {
                    if (item.getOrganization() != null && !item.getOrganization().isBlank()) {
                        score += 3;
                    }
                } else if (item.getType() == ProfileItemType.PROJECT) {
                    if (!jobTags.isBlank()) {
                        for (String tag : jobTags.split(",")) {
                            String t = tag.trim();
                            if (!t.isBlank() && itemDesc.contains(t)) {
                                score += 3;
                                rationale.add("Project: " + item.getTitle());
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 2. Candidate career summary relevance
        String summary = (profile.getSummary() != null) ? profile.getSummary().toLowerCase() : "";
        if (!summary.isBlank() && !jobTags.isBlank()) {
            for (String tag : jobTags.split(",")) {
                String t = tag.trim();
                if (!t.isBlank() && summary.contains(t)) {
                    score += 2;
                    break;
                }
            }
        }

        return Math.min(20, score);
    }
}

