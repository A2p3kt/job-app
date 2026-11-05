package dev.toluwalase.jobapp.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JPA repository for ApplicationKit entities.
 * Resides in the domain package per course architecture rules.
 */
public interface ApplicationKitRepository extends CrudRepository<ApplicationKit, Long> {

    /**
     * Retrieves all application kits created by a specific user, ordered by most recently updated.
     *
     * @param userId ID of the AppUser owner
     * @return List of ApplicationKits
     */
    List<ApplicationKit> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * Finds an existing application kit for a specific user and job posting pairing.
     * Used to prevent re-generating duplicates and to resume previous editing sessions.
     *
     * @param userId       ID of the AppUser
     * @param jobPostingId ID of the JobPosting
     * @return Optional containing the ApplicationKit if found
     */
    Optional<ApplicationKit> findByUserIdAndJobPostingId(Long userId, Long jobPostingId);
}

