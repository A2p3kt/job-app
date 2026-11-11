package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
 * Controller slice unit tests for JobWebController.
 * Verifies HTTP status codes, view resolutions, model bindings, and PRG redirect patterns.
 */
@ExtendWith(MockitoExtension.class)
public class JobWebControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private MatchingEngineService matchingEngineService;

    @Mock
    private JobScraperService jobScraperService;

    @Mock
    private JobPostingRepository jobPostingRepository;

    private MockMvc mockMvc;

    private MasterProfile testProfile;
    private JobPosting testJob;
    private JobMatchDto testMatch;

    @BeforeEach
    void setUp() {
        JobWebController controller = new JobWebController(
                profileService,
                matchingEngineService,
                jobScraperService,
                jobPostingRepository
        );
        org.springframework.web.servlet.view.InternalResourceViewResolver viewResolver =
                new org.springframework.web.servlet.view.InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        AppUser user = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testProfile = new MasterProfile(
                user,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358 40 123 4567",
                "Software Engineer",
                "Developer",
                "Java, Spring Boot",
                "Helsinki",
                "Backend"
        );

        testJob = new JobPosting(
                "job-001",
                JobCategory.TECHNOLOGY,
                "Junior Java Engineer",
                "Nordic Tech",
                "Helsinki",
                "Java development role",
                "Java, Spring Boot",
                "https://example.com/job",
                "JobTech",
                LocalDateTime.now()
        );

        testMatch = new JobMatchDto(testJob, 85, 30, 55, List.of("Specialization Match: Technology & IT", "Skill Match: Java"));
    }

    @Test
    @DisplayName("GET /jobs should default to candidate specialization and return 200 with jobs view")
    void testShowJobsDefaultSpecialization() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(matchingEngineService.rankJobsForCategory(testProfile, JobCategory.TECHNOLOGY))
                .thenReturn(List.of(testMatch));
        when(jobPostingRepository.count()).thenReturn(10L);

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs"))
                .andExpect(model().attributeExists("matches", "profile", "categories", "currentCategory", "showAll", "totalJobsCount"))
                .andExpect(model().attribute("currentCategory", JobCategory.TECHNOLOGY))
                .andExpect(model().attribute("showAll", false))
                .andExpect(model().attribute("totalJobsCount", 10L));

        verify(matchingEngineService).rankJobsForCategory(testProfile, JobCategory.TECHNOLOGY);
    }

    @Test
    @DisplayName("GET /jobs?showAll=true should rank across all industry categories")
    void testShowJobsAllCategories() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(matchingEngineService.rankJobsForProfile(testProfile)).thenReturn(List.of(testMatch));
        when(jobPostingRepository.count()).thenReturn(25L);

        mockMvc.perform(get("/jobs").param("showAll", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs"))
                .andExpect(model().attribute("showAll", true))
                .andExpect(model().attributeDoesNotExist("currentCategory"));

        verify(matchingEngineService).rankJobsForProfile(testProfile);
    }

    @Test
    @DisplayName("GET /jobs?category=MARKETING should filter jobs by the requested category")
    void testShowJobsCategoryFilter() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(matchingEngineService.rankJobsForCategory(testProfile, JobCategory.MARKETING))
                .thenReturn(Collections.emptyList());
        when(jobPostingRepository.count()).thenReturn(15L);

        mockMvc.perform(get("/jobs").param("category", "MARKETING"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs"))
                .andExpect(model().attribute("currentCategory", JobCategory.MARKETING))
                .andExpect(model().attribute("showAll", false));

        verify(matchingEngineService).rankJobsForCategory(testProfile, JobCategory.MARKETING);
    }

    @Test
    @DisplayName("POST /jobs/scrape should trigger manual scraping and redirect to /jobs with flash feedback")
    void testTriggerManualScrape() throws Exception {
        when(jobScraperService.triggerScrapeNow()).thenReturn(8);

        mockMvc.perform(post("/jobs/scrape"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(jobScraperService).triggerScrapeNow();
    }
}
