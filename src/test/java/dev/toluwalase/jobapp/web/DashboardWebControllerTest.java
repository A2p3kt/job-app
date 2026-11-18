package dev.toluwalase.jobapp.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;

/**
 * Unit tests for DashboardWebController.
 * Validates dashboard rendering, metric calculations, session resolution, and empty state behavior.
 */
@ExtendWith(MockitoExtension.class)
public class DashboardWebControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private TailoringService tailoringService;

    @Mock
    private JobPostingRepository jobPostingRepository;

    private MockMvc mockMvc;
    private AppUser testUser;
    private MasterProfile testProfile;
    private JobPosting testJob;

    @BeforeEach
    void setUp() {
        DashboardWebController controller = new DashboardWebController(
                profileService,
                tailoringService,
                jobPostingRepository
        );

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        testUser = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testUser.setId(1L);

        testProfile = new MasterProfile(
                testUser,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358 40 123 4567",
                "Senior Backend Engineer",
                "Bio",
                "Java, Spring",
                "Helsinki",
                "Backend"
        );
        testProfile.setId(10L);

        testJob = new JobPosting(
                "job-1",
                JobCategory.TECHNOLOGY,
                "Backend Developer",
                "Nordic Tech",
                "Helsinki",
                "Description",
                "Java",
                "https://example.com/job1",
                "JobTech",
                null
        );
        testJob.setId(101L);
    }

    @Test
    @DisplayName("GET /dashboard renders dashboard with populated metrics and kits")
    void testShowDashboardWithKits() throws Exception {
        ApplicationKit kit1 = new ApplicationKit(testUser, testJob, 95, "Match reasons", "CV", "Letter", "FINALIZED");
        kit1.setId(501L);
        ApplicationKit kit2 = new ApplicationKit(testUser, testJob, 85, "Match reasons", "CV", "Letter", "DRAFT");
        kit2.setId(502L);

        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(tailoringService.getKitsForUser(1L)).thenReturn(List.of(kit1, kit2));
        when(jobPostingRepository.count()).thenReturn(12L);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("totalKits", 2))
                .andExpect(model().attribute("finalizedKits", 1L))
                .andExpect(model().attribute("draftKits", 1L))
                .andExpect(model().attribute("avgMatchScore", 90))
                .andExpect(model().attribute("highestMatchScore", 95))
                .andExpect(model().attribute("totalJobsAvailable", 12L))
                .andExpect(model().attributeExists("profile", "user", "kits"));
    }

    @Test
    @DisplayName("GET /dashboard renders dashboard with zero kits gracefully")
    void testShowDashboardEmptyKits() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(tailoringService.getKitsForUser(1L)).thenReturn(Collections.emptyList());
        when(jobPostingRepository.count()).thenReturn(5L);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("totalKits", 0))
                .andExpect(model().attribute("finalizedKits", 0L))
                .andExpect(model().attribute("draftKits", 0L))
                .andExpect(model().attribute("avgMatchScore", 0))
                .andExpect(model().attribute("highestMatchScore", 0))
                .andExpect(model().attribute("totalJobsAvailable", 5L));
    }

    @Test
    @DisplayName("GET /dashboard resolves user identity from session")
    void testShowDashboardFromSession() throws Exception {
        AppUser sessionUser = new AppUser("sarah", "sarah@example.com", "hash", "LOCAL", "ROLE_USER");
        sessionUser.setId(2L);
        MasterProfile sessionProfile = new MasterProfile(sessionUser, JobCategory.HEALTHCARE, "Sarah M", "123", "Nurse", "Bio", "Care", "Helsinki", "Health");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "sarah");

        when(profileService.getOrCreateProfile("sarah")).thenReturn(sessionProfile);
        when(tailoringService.getKitsForUser(2L)).thenReturn(Collections.emptyList());
        when(jobPostingRepository.count()).thenReturn(3L);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("totalKits", 0));
    }
}
