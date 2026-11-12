package dev.toluwalase.jobapp.web;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.PdfExportService;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;

/**
 * Controller handling user interactions for the AI Application Kit workspace.
 * Supports on-demand kit generation, split-screen draft editing, and instant PDF downloads.
 */
@Controller
@RequestMapping("/kits")
public class KitWebController {

    private static final Logger log = LoggerFactory.getLogger(KitWebController.class);

    private final TailoringService tailoringService;
    private final PdfExportService pdfExportService;
    private final ProfileService profileService;

    /**
     * Explicit constructor injection without Lombok.
     */
    public KitWebController(TailoringService tailoringService,
                            PdfExportService pdfExportService,
                            ProfileService profileService) {
        this.tailoringService = tailoringService;
        this.pdfExportService = pdfExportService;
        this.profileService = profileService;
    }

    /**
     * Generates or resumes an ApplicationKit for the authenticated user and specified job.
     * Accessible via both GET (from job board links) and POST.
     *
     * @param jobId Target JobPosting ID
     * @param principal Current user principal
     * @param redirectAttributes Flash attributes for error notices
     * @return Redirect to the split-screen kit editor
     */
    @GetMapping("/generate/{jobId}")
    public String generateKitGet(@PathVariable Long jobId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        return processKitGeneration(jobId, principal, redirectAttributes);
    }

    @PostMapping("/generate/{jobId}")
    public String generateKitPost(@PathVariable Long jobId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        return processKitGeneration(jobId, principal, redirectAttributes);
    }

    private String processKitGeneration(Long jobId, Principal principal, RedirectAttributes redirectAttributes) {
        String username = (principal != null) ? principal.getName() : "tolu";
        try {
            MasterProfile profile = profileService.getOrCreateProfile(username);
            ApplicationKit kit = tailoringService.generateOrGetKit(profile.getUser().getId(), jobId);
            log.info("Redirecting user '{}' to kit editor for kit ID: {}", username, kit.getId());
            return "redirect:/kits/" + kit.getId() + "/edit";
        } catch (Exception ex) {
            log.error("Failed to generate application kit for job ID {}: {}", jobId, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Could not generate application kit: " + ex.getMessage());
            return "redirect:/jobs";
        }
    }

    /**
     * Renders the split-screen workspace displaying job requirements alongside
     * editable CV and Cover Letter drafts.
     *
     * @param kitId Primary key of the ApplicationKit
     * @param model Spring MVC model
     * @return View template name "kit-editor"
     */
    @GetMapping("/{kitId}/edit")
    public String editKit(@PathVariable Long kitId, Model model) {
        ApplicationKit kit = tailoringService.getKitById(kitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application kit not found: " + kitId));

        model.addAttribute("kit", kit);
        model.addAttribute("job", kit.getJobPosting());
        model.addAttribute("user", kit.getUser());

        log.debug("Loaded kit editor for Kit ID: {} (Job: '{}' at '{}')",
                kitId, kit.getJobPosting().getTitle(), kit.getJobPosting().getCompany());
        return "kit-editor";
    }

    /**
     * Persists user edits made to the CV and Cover Letter draft textareas.
     *
     * @param kitId Primary key of the ApplicationKit
     * @param tailoredCvContent Edited CV text/markdown
     * @param tailoredCoverLetterContent Edited Cover Letter text
     * @param redirectAttributes Flash feedback message
     * @return Redirect back to editor
     */
    @PostMapping("/{kitId}/save")
    public String saveKit(@PathVariable Long kitId,
                          @RequestParam("tailoredCvContent") String tailoredCvContent,
                          @RequestParam("tailoredCoverLetterContent") String tailoredCoverLetterContent,
                          RedirectAttributes redirectAttributes) {
        try {
            tailoringService.updateKit(kitId, tailoredCvContent, tailoredCoverLetterContent);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application kit saved successfully! Your tailored documents are updated.");
        } catch (Exception ex) {
            log.error("Failed to update ApplicationKit (ID: {}): {}", kitId, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to save changes: " + ex.getMessage());
        }

        return "redirect:/kits/" + kitId + "/edit";
    }

    /**
     * Generates and downloads the customized CV as a formatted PDF.
     *
     * @param kitId Primary key of the ApplicationKit
     * @return Streaming PDF response
     */
    @GetMapping("/{kitId}/pdf/cv")
    public ResponseEntity<byte[]> downloadCvPdf(@PathVariable Long kitId) {
        ApplicationKit kit = tailoringService.getKitById(kitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application kit not found: " + kitId));

        byte[] pdfBytes = pdfExportService.generateCvPdf(kit);
        String safeTitle = sanitizeFilename(kit.getJobPosting().getTitle());
        String filename = "CV-" + safeTitle + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    /**
     * Generates and downloads the customized Cover Letter as a formatted PDF.
     *
     * @param kitId Primary key of the ApplicationKit
     * @return Streaming PDF response
     */
    @GetMapping("/{kitId}/pdf/cover-letter")
    public ResponseEntity<byte[]> downloadCoverLetterPdf(@PathVariable Long kitId) {
        ApplicationKit kit = tailoringService.getKitById(kitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application kit not found: " + kitId));

        byte[] pdfBytes = pdfExportService.generateCoverLetterPdf(kit);
        String safeTitle = sanitizeFilename(kit.getJobPosting().getTitle());
        String filename = "CoverLetter-" + safeTitle + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    private String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "Application";
        }
        return input.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("_{2,}", "_");
    }
}

