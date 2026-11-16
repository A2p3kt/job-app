package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.PdfExportService;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;
import tools.jackson.databind.ObjectMapper;

/**
 * Slice unit test suite for KitApiController.
 */
@ExtendWith(MockitoExtension.class)
public class KitApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TailoringService tailoringService;

    @Mock
    private PdfExportService pdfExportService;

    @Mock
    private ProfileService profileService;

    @Mock
    private ApplicationKitRepository kitRepository;

    private AppUser testUser;
    private MasterProfile testProfile;
    private JobPosting testJob;
    private ApplicationKit testKit;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        KitApiController controller = new KitApiController(
                tailoringService,
                pdfExportService,
                profileService,
                kitRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        testUser = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testUser.setId(1L);

        testProfile = new MasterProfile(testUser, JobCategory.TECHNOLOGY, "Tolu", "123", "Dev", "Summary", "Java", "Helsinki", "Dev");
        testJob = new JobPosting("job-1", JobCategory.TECHNOLOGY, "Backend Dev", "Acme", "Remote", "Java role", "Java", "url", "JobTech", LocalDateTime.now());
        testJob.setId(10L);

        testKit = new ApplicationKit(testUser, testJob, 92, "Java match", "# My CV", "Dear Acme", "DRAFT");
        testKit.setId(5L);
    }

    @Test
    @DisplayName("GET /api/v1/kits returns list of user's application kits")
    void testGetUserKits() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(kitRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(testKit));

        mockMvc.perform(get("/api/v1/kits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchScore").value(92))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/v1/kits/generate/{jobId} creates kit and returns 201 Created")
    void testGenerateKit() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(tailoringService.generateOrGetKit(eq(1L), eq(10L))).thenReturn(testKit);

        mockMvc.perform(post("/api/v1/kits/generate/10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.matchScore").value(92));
    }

    @Test
    @DisplayName("GET /api/v1/kits/{kitId} returns 200 OK when kit exists")
    void testGetKit() throws Exception {
        when(tailoringService.getKitById(5L)).thenReturn(Optional.of(testKit));

        mockMvc.perform(get("/api/v1/kits/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/kits/{kitId} updates kit text and returns 200 OK")
    void testUpdateKit() throws Exception {
        when(tailoringService.updateKit(eq(5L), any(), any())).thenReturn(testKit);

        Map<String, String> payload = Map.of(
                "tailoredCvContent", "# Updated CV",
                "tailoredCoverLetterContent", "Updated Letter"
        );

        mockMvc.perform(put("/api/v1/kits/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(tailoringService).updateKit(eq(5L), eq("# Updated CV"), eq("Updated Letter"));
    }

    @Test
    @DisplayName("GET /api/v1/kits/{kitId}/pdf/cv returns PDF byte stream")
    void testDownloadCvPdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock cv pdf".getBytes();
        when(tailoringService.getKitById(5L)).thenReturn(Optional.of(testKit));
        when(pdfExportService.generateCvPdf(testKit)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/kits/5/pdf/cv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"CV-Backend_Dev.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("GET /api/v1/kits/{kitId}/pdf/cover-letter returns PDF byte stream")
    void testDownloadCoverLetterPdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock cover letter pdf".getBytes();
        when(tailoringService.getKitById(5L)).thenReturn(Optional.of(testKit));
        when(pdfExportService.generateCoverLetterPdf(testKit)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/kits/5/pdf/cover-letter"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"Cover-Letter-Backend_Dev.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
