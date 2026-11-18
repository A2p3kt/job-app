package dev.toluwalase.jobapp.web;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.ProfileService;
import dev.toluwalase.jobapp.service.TailoringService;
import jakarta.servlet.http.HttpSession;

/**
 * Controller handling web requests for the candidate application dashboard.
 * Displays an overview of saved application kits, match quality metrics,
 * career specialization status, and shortcuts to the job board and master CV editor.
 */
@Controller
public class DashboardWebController {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebController.class);

    private final ProfileService profileService;
    private final TailoringService tailoringService;
    private final JobPostingRepository jobPostingRepository;

    public DashboardWebController(ProfileService profileService,
                                  TailoringService tailoringService,
                                  JobPostingRepository jobPostingRepository) {
        this.profileService = profileService;
        this.tailoringService = tailoringService;
        this.jobPostingRepository = jobPostingRepository;
    }

    private String resolveUsername(Principal principal, HttpSession session) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (session != null && session.getAttribute("username") != null) {
            return (String) session.getAttribute("username");
        }
        return "tolu";
    }

    /**
     * Renders the candidate dashboard with overview metrics, saved application kits,
     * profile summary, and quick navigation links.
     *
     * @param model Spring MVC model
     * @param principal Current user principal
     * @param session Current HTTP session
     * @return Template view name "dashboard"
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model, Principal principal, HttpSession session) {
        String username = resolveUsername(principal, session);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        List<ApplicationKit> kits = tailoringService.getKitsForUser(profile.getUser().getId());

        int totalKits = kits.size();
        long finalizedKits = kits.stream().filter(k -> "FINALIZED".equalsIgnoreCase(k.getStatus())).count();
        long draftKits = kits.stream().filter(k -> "DRAFT".equalsIgnoreCase(k.getStatus())).count();
        int avgMatchScore = kits.isEmpty() ? 0 : (int) Math.round(kits.stream().mapToInt(ApplicationKit::getMatchScore).average().orElse(0.0));
        int highestMatchScore = kits.stream().mapToInt(ApplicationKit::getMatchScore).max().orElse(0);
        long totalJobsAvailable = jobPostingRepository.count();

        model.addAttribute("profile", profile);
        model.addAttribute("user", profile.getUser());
        model.addAttribute("kits", kits);
        model.addAttribute("totalKits", totalKits);
        model.addAttribute("finalizedKits", finalizedKits);
        model.addAttribute("draftKits", draftKits);
        model.addAttribute("avgMatchScore", avgMatchScore);
        model.addAttribute("highestMatchScore", highestMatchScore);
        model.addAttribute("totalJobsAvailable", totalJobsAvailable);

        log.debug("Rendered dashboard for user '{}': {} application kits found", username, totalKits);
        return "dashboard";
    }
}
