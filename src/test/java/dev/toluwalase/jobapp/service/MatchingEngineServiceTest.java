package dev.toluwalase.jobapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Unit test suite for MatchingEngineService.
 * Validates two-tier matching calculations, category alignment, penalty factors,
 * and ranking behavior.
 */
@ExtendWith(MockitoExtension.class)
public class MatchingEngineServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    private MatchingEngineService matchingEngineService;

    private MasterProfile techProfile;
    private JobPosting matchingTechJob;
    private JobPosting nonMatchingCleaningJob;

    @BeforeEach
    void setUp() {
        matchingEngineService = new MatchingEngineService(jobPostingRepository);

        AppUser user = new AppUser("testuser", "test@example.com", "hash", "LOCAL", "ROLE_USER");
        techProfile = new MasterProfile(
                user,
                JobCategory.TECHNOLOGY,
                "Alex Developer",
                "+358 40 999 8888",
                "Full Stack Developer",
                "Experienced software developer skilled in Java, Spring Boot, PostgreSQL, and Docker.",
                "Java, Spring Boot, PostgreSQL, Docker, AWS, React, Git",
                "Helsinki, Espoo, Remote",
                "Backend, Software Engineer, Cloud"
        );

        techProfile.addItem(new ProfileItem(
                techProfile,
                ProfileItemType.EXPERIENCE,
                "Backend Engineer",
                "Acme Corp",
                "Helsinki, Finland",
                "2023-01",
                "2024-01",
                "Built microservices with Java, Spring Boot, and PostgreSQL in Docker containers."
        ));

        techProfile.addItem(new ProfileItem(
                techProfile,
                ProfileItemType.CERTIFICATION,
                "AWS Certified Cloud Practitioner",
                "Amazon",
                "Remote",
                "2023-05",
                "2026-05",
                "Cloud fundamentals"
        ));

        matchingTechJob = new JobPosting(
                "job-tech-01",
                JobCategory.TECHNOLOGY,
                "Senior Backend Software Engineer",
                "FinTech Solutions",
                "Helsinki, Finland (Hybrid)",
                "We need a backend developer with Java, Spring Boot, PostgreSQL, and Docker experience.",
                "Java, Spring Boot, PostgreSQL, Docker, AWS",
                "https://example.com/job1",
                "ManualTest",
                LocalDateTime.now()
        );

        nonMatchingCleaningJob = new JobPosting(
                "job-clean-01",
                JobCategory.SERVICE_OPERATIONS,
                "Commercial Cleaner & Facility Specialist",
                "CleanCorp",
                "Turku, Finland",
                "Commercial sanitization and floor maintenance.",
                "Commercial Cleaning, Sanitization, Equipment Maintenance",
                "https://example.com/clean1",
                "ManualTest",
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("scoreJob should calculate a high match score for well-aligned candidate and job")
    void testScoreJobHighMatch() {
        JobMatchDto match = matchingEngineService.scoreJob(techProfile, matchingTechJob);

        assertNotNull(match);
        assertTrue(match.getMatchScore() >= 80, "Score should be >= 80 for strong alignment, was: " + match.getMatchScore());
        assertTrue(match.getPreferenceScore() > 0, "Preference score should be positive");
        assertTrue(match.getEvidenceScore() > 0, "Evidence score should be positive");

        List<String> reasons = match.getMatchReasons();
        assertFalse(reasons.isEmpty());
        assertTrue(reasons.stream().anyMatch(r -> r.contains("Java")));
        assertTrue(reasons.stream().anyMatch(r -> r.contains("Location Match")));
        assertTrue(reasons.stream().anyMatch(r -> r.contains("Specialization Match")));
    }

    @Test
    @DisplayName("scoreJob should apply a cross-industry penalty for jobs outside the candidate specialization")
    void testScoreJobCrossIndustryPenalty() {
        JobMatchDto cleanMatch = matchingEngineService.scoreJob(techProfile, nonMatchingCleaningJob);

        assertNotNull(cleanMatch);
        assertTrue(cleanMatch.getMatchScore() < 30, "Score should be low for cross-industry mismatch, was: " + cleanMatch.getMatchScore());
        assertTrue(cleanMatch.getMatchReasons().stream().anyMatch(r -> r.contains("Cross-Industry Opportunity")));
    }

    @Test
    @DisplayName("rankJobsForProfile should sort jobs descending by match score")
    void testRankJobsForProfileSorting() {
        List<JobPosting> jobs = List.of(nonMatchingCleaningJob, matchingTechJob);

        List<JobMatchDto> ranked = matchingEngineService.rankJobsForProfile(techProfile, jobs);

        assertEquals(2, ranked.size());
        assertEquals("job-tech-01", ranked.get(0).getJob().getExternalId(), "Higher matching job should rank first");
        assertEquals("job-clean-01", ranked.get(1).getJob().getExternalId(), "Lower matching job should rank second");
        assertTrue(ranked.get(0).getMatchScore() > ranked.get(1).getMatchScore());
    }

    @Test
    @DisplayName("rankJobsForProfile from repository should fetch all jobs and rank them")
    void testRankJobsFromRepository() {
        when(jobPostingRepository.findByOrderByScrapedAtDesc()).thenReturn(List.of(matchingTechJob, nonMatchingCleaningJob));

        List<JobMatchDto> ranked = matchingEngineService.rankJobsForProfile(techProfile);

        assertEquals(2, ranked.size());
        assertEquals("job-tech-01", ranked.get(0).getJob().getExternalId());
        verify(jobPostingRepository).findByOrderByScrapedAtDesc();
    }

    @Test
    @DisplayName("rankJobsForCategory should query category-specific jobs")
    void testRankJobsForCategory() {
        when(jobPostingRepository.findByCategoryOrderByScrapedAtDesc(JobCategory.TECHNOLOGY))
                .thenReturn(List.of(matchingTechJob));

        List<JobMatchDto> ranked = matchingEngineService.rankJobsForCategory(techProfile, JobCategory.TECHNOLOGY);

        assertEquals(1, ranked.size());
        assertEquals("job-tech-01", ranked.get(0).getJob().getExternalId());
        verify(jobPostingRepository).findByCategoryOrderByScrapedAtDesc(JobCategory.TECHNOLOGY);
    }

    @Test
    @DisplayName("scoreJob should handle null profile or null job without throwing an exception")
    void testScoreJobNullSafety() {
        JobMatchDto match1 = matchingEngineService.scoreJob(null, matchingTechJob);
        assertNotNull(match1);
        assertEquals(0, match1.getMatchScore());

        JobMatchDto match2 = matchingEngineService.scoreJob(techProfile, null);
        assertNotNull(match2);
        assertEquals(0, match2.getMatchScore());
    }
}

