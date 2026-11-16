package dev.toluwalase.jobapp.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.service.JobScraperService;
import dev.toluwalase.jobapp.service.MatchingEngineService;
import dev.toluwalase.jobapp.service.ProfileService;

/**
 * RESTful API controller providing endpoints for job opportunity recommendations,
 * category filtering, and on-demand scraping triggers under /api/v1/jobs.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobApiController {

    private static final Logger log = LoggerFactory.getLogger(JobApiController.class);

    private final JobPostingRepository jobPostingRepository;
    private final MatchingEngineService matchingEngineService;
    private final ProfileService profileService;
    private final JobScraperService jobScraperService;

    public JobApiController(JobPostingRepository jobPostingRepository,
                            MatchingEngineService matchingEngineService,
                            ProfileService profileService,
                            JobScraperService jobScraperService) {
        this.jobPostingRepository = jobPostingRepository;
        this.matchingEngineService = matchingEngineService;
        this.profileService = profileService;
        this.jobScraperService = jobScraperService;
    }

    private String resolveUsername(Principal principal) {
        return (principal != null) ? principal.getName() : "tolu";
    }

    /**
     * Retrieves ranked job recommendations evaluated against the candidate's MasterProfile.
     *
     * @param category  Optional category filter parameter
     * @param principal Authenticated user security principal
     * @return List of ranked JobMatchDto records
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<JobMatchDto>> getRecommendations(
            @RequestParam(required = false) JobCategory category,
            Principal principal) {
        String username = resolveUsername(principal);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        JobCategory filter = (category != null) ? category : profile.getSpecialization();
        List<JobMatchDto> matches = matchingEngineService.rankJobsForCategory(profile, filter);
        return ResponseEntity.ok(matches);
    }

    /**
     * Retrieves a single job posting by its primary database ID.
     *
     * @param id JobPosting entity ID
     * @return 200 OK with JobPosting, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobPosting> getJobById(@PathVariable Long id) {
        return jobPostingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Triggers an on-demand multi-source scraper run across JobTech, Remotive, and Arbeitnow.
     *
     * @return Status report with count of newly ingested postings
     */
    @PostMapping("/scrape")
    public ResponseEntity<Map<String, Object>> triggerScrape() {
        log.info("API trigger for multi-source scraper initiated.");
        int newPostings = jobScraperService.triggerScrapeNow();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "newPostings", newPostings,
                "message", "Ingestion complete: " + newPostings + " new postings saved."
        ));
    }
}
