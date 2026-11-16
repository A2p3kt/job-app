package dev.toluwalase.jobapp.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.PdfExportService;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;

/**
 * RESTful API controller providing endpoints for AI Application Kit generation,
 * draft retrieval and editing, and binary PDF export under /api/v1/kits.
 */
@RestController
@RequestMapping("/api/v1/kits")
public class KitApiController {

    private static final Logger log = LoggerFactory.getLogger(KitApiController.class);

    private final TailoringService tailoringService;
    private final PdfExportService pdfExportService;
    private final ProfileService profileService;
    private final ApplicationKitRepository kitRepository;

    public KitApiController(TailoringService tailoringService,
                            PdfExportService pdfExportService,
                            ProfileService profileService,
                            ApplicationKitRepository kitRepository) {
        this.tailoringService = tailoringService;
        this.pdfExportService = pdfExportService;
        this.profileService = profileService;
        this.kitRepository = kitRepository;
    }

    private String resolveUsername(Principal principal) {
        return (principal != null) ? principal.getName() : "tolu";
    }

    /**
     * Retrieves all ApplicationKits owned by the current candidate.
     *
     * @param principal Authenticated user security principal
     * @return List of ApplicationKit entities
     */
    @GetMapping
    public ResponseEntity<List<ApplicationKit>> getUserKits(Principal principal) {
        String username = resolveUsername(principal);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        List<ApplicationKit> kits = kitRepository.findByUserIdOrderByUpdatedAtDesc(profile.getUser().getId());
        return ResponseEntity.ok(kits);
    }

    /**
     * Generates or fetches an existing ApplicationKit for the user targeting a specific job.
     *
     * @param jobId     Target JobPosting primary key
     * @param principal Authenticated user security principal
     * @return 201 Created with ApplicationKit entity
     */
    @PostMapping("/generate/{jobId}")
    public ResponseEntity<ApplicationKit> generateKit(@PathVariable Long jobId, Principal principal) {
        String username = resolveUsername(principal);
        log.info("API request to generate ApplicationKit for user '{}' targeting job ID {}", username, jobId);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        ApplicationKit kit = tailoringService.generateOrGetKit(profile.getUser().getId(), jobId);
        return ResponseEntity.status(HttpStatus.CREATED).body(kit);
    }

    /**
     * Retrieves an ApplicationKit by its primary key.
     *
     * @param kitId Primary key of the ApplicationKit
     * @return 200 OK with kit, or 404 Not Found
     */
    @GetMapping("/{kitId}")
    public ResponseEntity<ApplicationKit> getKit(@PathVariable Long kitId) {
        return tailoringService.getKitById(kitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates tailored CV and Cover Letter text for an existing ApplicationKit.
     *
     * @param kitId   Primary key of the ApplicationKit
     * @param payload Map containing 'tailoredCvContent' and/or 'tailoredCoverLetterContent'
     * @return 200 OK with the updated ApplicationKit
     */
    @PutMapping("/{kitId}")
    public ResponseEntity<ApplicationKit> updateKit(@PathVariable Long kitId,
                                                    @RequestBody Map<String, String> payload) {
        String cvContent = payload.getOrDefault("tailoredCvContent", "");
        String coverLetterContent = payload.getOrDefault("tailoredCoverLetterContent", "");
        log.info("API request to update ApplicationKit (ID: {})", kitId);
        ApplicationKit updated = tailoringService.updateKit(kitId, cvContent, coverLetterContent);
        return ResponseEntity.ok(updated);
    }

    /**
     * Exports and streams the candidate's tailored CV as an ATS-friendly PDF.
     *
     * @param kitId Primary key of the ApplicationKit
     * @return PDF binary byte stream with Content-Disposition header
     */
    @GetMapping("/{kitId}/pdf/cv")
    public ResponseEntity<byte[]> downloadCvPdf(@PathVariable Long kitId) {
        return tailoringService.getKitById(kitId)
                .map(kit -> {
                    byte[] pdf = pdfExportService.generateCvPdf(kit);
                    String sanitized = kit.getJobPosting().getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"CV-" + sanitized + ".pdf\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Exports and streams the candidate's tailored Cover Letter as a PDF.
     *
     * @param kitId Primary key of the ApplicationKit
     * @return PDF binary byte stream with Content-Disposition header
     */
    @GetMapping("/{kitId}/pdf/cover-letter")
    public ResponseEntity<byte[]> downloadCoverLetterPdf(@PathVariable Long kitId) {
        return tailoringService.getKitById(kitId)
                .map(kit -> {
                    byte[] pdf = pdfExportService.generateCoverLetterPdf(kit);
                    String sanitized = kit.getJobPosting().getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Cover-Letter-" + sanitized + ".pdf\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
