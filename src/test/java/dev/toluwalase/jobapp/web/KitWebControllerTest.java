package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.PdfExportService;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;

/**
 * Controller slice unit tests for KitWebController.
 * Validates endpoints for generating kits, editing drafts, saving changes,
 * and downloading customized PDF exports.
 */
@ExtendWith(MockitoExtension.class)
public class KitWebControllerTest {

    @Mock
    private TailoringService tailoringService;

    @Mock
    private PdfExportService pdfExportService;

    @Mock
    private ProfileService profileService;

    private MockMvc mockMvc;

    private AppUser testUser;
    private MasterProfile testProfile;
    private JobPosting testJob;
    private ApplicationKit testKit;

    @BeforeEach
    void setUp() {
        KitWebController controller = new KitWebController(tailoringService, pdfExportService, profileService);

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
                "Senior Software Engineer",
                "Bio",
                "Java, Spring Boot",
                "Helsinki",
                "Backend"
        );
        testProfile.setId(10L);

        testJob = new JobPosting(
                "remotive-50",
                JobCategory.TECHNOLOGY,
                "Senior Java Engineer",
                "Acme Global",
                "Remote",
                "Job description details",
                "Java, Spring Boot",
                "https://example.com/job/50",
                "Remotive",
                LocalDateTime.now()
        );
        testJob.setId(50L);

        testKit = new ApplicationKit(
                testUser,
                testJob,
                94,
                "High skill overlap",
                "# CV Content",
                "Cover Letter Content",
                "DRAFT"
        );
        testKit.setId(500L);
    }

    @Test
    @DisplayName("GET /kits/generate/{jobId} generates kit and redirects to editor")
    void testGenerateKitGet() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(tailoringService.generateOrGetKit(1L, 50L)).thenReturn(testKit);

        mockMvc.perform(get("/kits/generate/50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kits/500/edit"));

        verify(tailoringService).generateOrGetKit(1L, 50L);
    }

    @Test
    @DisplayName("POST /kits/generate/{jobId} generates kit and redirects to editor")
    void testGenerateKitPost() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(tailoringService.generateOrGetKit(1L, 50L)).thenReturn(testKit);

        mockMvc.perform(post("/kits/generate/50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kits/500/edit"));

        verify(tailoringService).generateOrGetKit(1L, 50L);
    }

    @Test
    @DisplayName("GET /kits/{kitId}/edit renders kit-editor view with bound model attributes")
    void testEditKit() throws Exception {
        when(tailoringService.getKitById(500L)).thenReturn(Optional.of(testKit));

        mockMvc.perform(get("/kits/500/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("kit-editor"))
                .andExpect(model().attributeExists("kit"))
                .andExpect(model().attributeExists("job"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("GET /kits/{kitId}/edit returns 404 when kit does not exist")
    void testEditKitNotFound() throws Exception {
        when(tailoringService.getKitById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/kits/999/edit"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /kits/{kitId}/save updates kit and redirects with flash message")
    void testSaveKit() throws Exception {
        mockMvc.perform(post("/kits/500/save")
                        .param("tailoredCvContent", "Updated CV")
                        .param("tailoredCoverLetterContent", "Updated Letter"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kits/500/edit"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(tailoringService).updateKit(500L, "Updated CV", "Updated Letter");
    }

    @Test
    @DisplayName("GET /kits/{kitId}/pdf/cv streams downloadable application/pdf with content disposition header")
    void testDownloadCvPdf() throws Exception {
        byte[] dummyPdf = "%PDF-1.4 dummy cv binary stream".getBytes();
        when(tailoringService.getKitById(500L)).thenReturn(Optional.of(testKit));
        when(pdfExportService.generateCvPdf(testKit)).thenReturn(dummyPdf);

        mockMvc.perform(get("/kits/500/pdf/cv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"CV-Senior_Java_Engineer.pdf\""))
                .andExpect(content().bytes(dummyPdf));
    }

    @Test
    @DisplayName("GET /kits/{kitId}/pdf/cover-letter streams downloadable application/pdf with content disposition header")
    void testDownloadCoverLetterPdf() throws Exception {
        byte[] dummyPdf = "%PDF-1.4 dummy letter binary stream".getBytes();
        when(tailoringService.getKitById(500L)).thenReturn(Optional.of(testKit));
        when(pdfExportService.generateCoverLetterPdf(testKit)).thenReturn(dummyPdf);

        mockMvc.perform(get("/kits/500/pdf/cover-letter"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"CoverLetter-Senior_Java_Engineer.pdf\""))
                .andExpect(content().bytes(dummyPdf));
    }
}

