package dev.toluwalase.jobapp.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JPA repository for JobPosting entities.
 * Resides in the domain package per course architecture rules.
 */
public interface JobPostingRepository extends CrudRepository<JobPosting, Long> {

    /**
     * Checks if a job posting with the specified external identifier already exists.
     * Used by the scraper service to avoid duplicate entries.
     *
     * @param externalId External identifier from source API
     * @return true if an entry exists, false otherwise
     */
    boolean existsByExternalId(String externalId);

    /**
     * Retrieves all job postings ordered chronologically with the latest scraped jobs first.
     *
     * @return List of job postings sorted by scrapedAt descending
     */
    List<JobPosting> findByOrderByScrapedAtDesc();

    /**
     * Retrieves all job postings matching a specific industry category, ordered with latest first.
     *
     * @param category Target industry category enum
     * @return List of matching job postings
     */
    List<JobPosting> findByCategoryOrderByScrapedAtDesc(JobCategory category);

    /**
     * Finds a job posting by its external identifier.
     *
     * @param externalId External source identifier
     * @return Optional containing the job posting if found
     */
    Optional<JobPosting> findByExternalId(String externalId);
}
