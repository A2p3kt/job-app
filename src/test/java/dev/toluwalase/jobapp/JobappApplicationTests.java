package dev.toluwalase.jobapp;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.service.MatchingEngineService;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;
import dev.toluwalase.jobapp.web.DashboardWebController;
import dev.toluwalase.jobapp.web.JobApiController;
import dev.toluwalase.jobapp.web.KitApiController;
import dev.toluwalase.jobapp.web.ProfileApiController;

/**
 * End-to-end integration test validating full Spring application context loading
 * and dependency injection across domain, service, and web layers.
 */
@SpringBootTest
class JobappApplicationTests {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MatchingEngineService matchingEngineService;

    @Autowired
    private TailoringService tailoringService;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private ApplicationKitRepository applicationKitRepository;

    @Autowired
    private MasterProfileRepository profileRepository;

    @Autowired
    private DashboardWebController dashboardWebController;

    @Autowired
    private ProfileApiController profileApiController;

    @Autowired
    private JobApiController jobApiController;

    @Autowired
    private KitApiController kitApiController;

    @Test
    @DisplayName("Context loads successfully and all key service/web beans are injected")
    void contextLoads() {
        assertNotNull(profileService);
        assertNotNull(matchingEngineService);
        assertNotNull(tailoringService);
        assertNotNull(jobPostingRepository);
        assertNotNull(applicationKitRepository);
        assertNotNull(profileRepository);
        assertNotNull(dashboardWebController);
        assertNotNull(profileApiController);
        assertNotNull(jobApiController);
        assertNotNull(kitApiController);
    }
}
