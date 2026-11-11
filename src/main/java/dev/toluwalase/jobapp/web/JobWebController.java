package dev.toluwalase.jobapp.web;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.JobScraperService;
import dev.toluwalase.jobapp.service.MatchingEngineService;
import dev.toluwalase.jobapp.service.ProfileService;

/**
 * Web controller handling Thymeleaf HTTP requests for browsing ranked job
 * recommendations,
 * filtering market opportunities by industry specialization, and triggering
 * immediate multi-source scrapes.
 */
@RequestMapping("/jobs")
@Controller
public class JobWebController {

    private static final Logger log = LoggerFactory.getLogger(JobWebController.class);

    private final ProfileService profileService;
    private final MatchingEngineService matchingEngineService;
    private final JobScraperService jobScraperService;
    private final JobPostingRepository jobPostingRepository;

    /**
     * Explicit constructor injection without Lombok.
     *
     * @param profileService        Service for retrieving candidate profile &
     *                              preferences
     * @param matchingEngineService Two-tier 0–100 matching engine service
     * @param jobScraperService     Multi-source scraping service
     * @param jobPostingRepository  Repository for job counts and listings
     */
    public JobWebController(ProfileService profileService,
            MatchingEngineService matchingEngineService,
            JobScraperService jobScraperService,
            JobPostingRepository jobPostingRepository) {
        this.profileService = profileService;
        this.matchingEngineService = matchingEngineService;
        this.jobScraperService = jobScraperService;
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Displays ranked job postings scored against the authenticated user's
     * MasterProfile.
     * Defaults to filtering by the user's active career specialization.
     *
     * @param categoryFilter Optional category override query parameter
     * @param showAll        Whether to display all jobs across all industry
     *                       verticals
     * @param model          Spring MVC UI Model
     * @param principal      Authenticated user principal (falls back to default
     *                       seed user "tolu")
     * @return Template view name "jobs"
     */
    @GetMapping
    public String showJobs(@RequestParam(value = "category", required = false) JobCategory categoryFilter,
            @RequestParam(value = "showAll", required = false, defaultValue = "false") boolean showAll,
            Model model,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "tolu";
        MasterProfile profile = profileService.getOrCreateProfile(username);

        JobCategory activeCategory = null;
        List<JobMatchDto> matches;

        if (showAll) {
            matches = matchingEngineService.rankJobsForProfile(profile);
        } else if (categoryFilter != null) {
            activeCategory = categoryFilter;
            matches = matchingEngineService.rankJobsForCategory(profile, categoryFilter);
        } else {
            // Default to candidate's active specialization
            activeCategory = (profile.getSpecialization() != null)
                    ? profile.getSpecialization()
                    : JobCategory.TECHNOLOGY;
            matches = matchingEngineService.rankJobsForCategory(profile, activeCategory);
        }

        model.addAttribute("matches", matches);
        model.addAttribute("profile", profile);
        model.addAttribute("categories", JobCategory.values());
        model.addAttribute("currentCategory", activeCategory);
        model.addAttribute("showAll", showAll);
        model.addAttribute("totalJobsCount", jobPostingRepository.count());

        log.debug("Rendered {} ranked jobs for user '{}' (specialization: {}, activeCategory: {})",
                matches.size(), username, profile.getSpecialization(), activeCategory);

        return "jobs";
    }

    /**
     * Triggers an immediate, synchronous multi-source scraping run across JobTech,
     * The Muse,
     * Remotive, and Arbeitnow, refreshing the database with latest market listings.
     *
     * @param redirectAttributes Flash attributes for feedback notifications
     * @return Redirect to /jobs (PRG pattern)
     */
    @PostMapping("/scrape")
    public String triggerManualScrape(RedirectAttributes redirectAttributes) {
        log.info("Manual marketplace sync initiated by user.");
        try {
            int newJobs = jobScraperService.triggerScrapeNow();
            if (newJobs > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Marketplace sync complete! Successfully ingested " + newJobs
                                + " new job postings from JobTech, The Muse, Remotive, and Arbeitnow.");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Marketplace sync complete. All external listings are already indexed and up to date!");
            }
        } catch (Exception ex) {
            log.error("Manual scrape execution encountered an error: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Marketplace sync completed with warnings: " + ex.getMessage());
        }

        return "redirect:/jobs";
    }
}
