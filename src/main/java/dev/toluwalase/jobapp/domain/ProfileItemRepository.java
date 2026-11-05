package dev.toluwalase.jobapp.domain;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JPA repository for ProfileItem entities.
 * Resides in the domain package per course architecture rules.
 */
public interface ProfileItemRepository extends CrudRepository<ProfileItem, Long> {

    /**
     * Retrieves all profile items belonging to a given MasterProfile ID.
     *
     * @param profileId ID of the parent MasterProfile
     * @return List of matching ProfileItems
     */
    List<ProfileItem> findByProfileId(Long profileId);

    /**
     * Retrieves all profile items of a specific category (e.g. EXPERIENCE, EDUCATION)
     * belonging to a given MasterProfile ID.
     *
     * @param profileId ID of the parent MasterProfile
     * @param type      Category enum filter
     * @return List of matching ProfileItems
     */
    List<ProfileItem> findByProfileIdAndType(Long profileId, ProfileItemType type);
}

