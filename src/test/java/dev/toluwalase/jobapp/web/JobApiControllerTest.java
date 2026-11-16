package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.JobScraperService;
import dev.toluwalase.jobapp.service.MatchingEngineService;
import dev.toluwalase.jobapp.service.ProfileService;

/**
 * Slice unit test suite for JobApiController.
 */
@ExtendWith(MockitoExtension.class)
public class JobApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private MatchingEngineService matchingEngineService;

    @Mock
    private ProfileService profileService;

    @Mock
    private JobScraperService jobScraperService;

    private MasterProfile testProfile;
    private JobPosting testJob;

    @BeforeEach
    void setUp() {
        JobApiController controller = new JobApiController(
                jobPostingRepository,
                matchingEngineService,
                profileService,
                jobScraperService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        AppUser user = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testProfile = new MasterProfile(user, JobCategory.TECHNOLOGY, "Tolu", "123", "Dev", "Summary", "Java", "Helsinki", "Dev");
        testJob = new JobPosting("job-101", JobCategory.TECHNOLOGY, "Software Engineer", "Acme", "Helsinki", "Java role", "Java", "url", "JobTech", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/v1/jobs/recommendations returns list of ranked match DTOs")
    void testGetRecommendations() throws Exception {
        JobMatchDto match = new JobMatchDto(testJob, 85, 35, 50, List.of("Java", "Spring Boot"));
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(matchingEngineService.rankJobsForCategory(eq(testProfile), eq(JobCategory.TECHNOLOGY)))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/v1/jobs/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchScore").value(85))
                .andExpect(jsonPath("$[0].job.title").value("Software Engineer"));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} returns 200 OK when job exists")
    void testGetJobById() throws Exception {
        when(jobPostingRepository.findById(101L)).thenReturn(Optional.of(testJob));

        mockMvc.perform(get("/api/v1/jobs/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("job-101"))
                .andExpect(jsonPath("$.company").value("Acme"));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} returns 404 Not Found when absent")
    void testGetJobByIdNotFound() throws Exception {
        when(jobPostingRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/jobs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/jobs/scrape triggers scraper and returns status report")
    void testTriggerScrape() throws Exception {
        when(jobScraperService.triggerScrapeNow()).thenReturn(14);

        mockMvc.perform(post("/api/v1/jobs/scrape"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.newPostings").value(14));

        verify(jobScraperService).triggerScrapeNow();
    }
}
