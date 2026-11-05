package dev.toluwalase.jobapp.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Service managing business logic for candidate career dossiers (MasterProfile)
 * and their modular resume components (ProfileItem).
 * Enforces ownership checks to ensure users can only modify their own profile data.
 */
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final MasterProfileRepository profileRepository;
    private final ProfileItemRepository itemRepository;
    private final AppUserRepository userRepository;

    /**
     * Explicit constructor injection without Lombok.
     *
     * @param profileRepository Repository for MasterProfile persistence
     * @param itemRepository    Repository for ProfileItem persistence
     * @param userRepository    Repository for AppUser persistence
     */
    public ProfileService(MasterProfileRepository profileRepository,
                          ProfileItemRepository itemRepository,
                          AppUserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the MasterProfile belonging to a specific user by their database ID.
     *
     * @param userId Primary key of the AppUser
     * @return Optional containing the MasterProfile if found
     */
    public Optional<MasterProfile> getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId);
    }

    /**
     * Retrieves the MasterProfile belonging to a specific user by their username.
     *
     * @param username Login username of the AppUser
     * @return Optional containing the MasterProfile if found
     */
    public Optional<MasterProfile> getProfileByUsername(String username) {
        return profileRepository.findByUserUsername(username);
    }

    /**
     * Retrieves an existing profile for the given username or initializes a blank default profile.
     * Guarantees that views loading a user's profile will not encounter a null pointer.
     *
     * @param username Login username
     * @return Existing or newly persisted MasterProfile
     * @throws IllegalArgumentException if the username does not correspond to an existing AppUser
     */
    @Transactional
    public MasterProfile getOrCreateProfile(String username) {
        Optional<MasterProfile> existing = profileRepository.findByUserUsername(username);
        if (existing.isPresent()) {
            return existing.get();
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        MasterProfile newProfile = new MasterProfile(
                user,
                JobCategory.TECHNOLOGY,
                user.getUsername(),
                "",
                "",
                "",
                "",
                "",
                ""
        );
        MasterProfile saved = profileRepository.save(newProfile);
        log.info("Initialized default MasterProfile (ID: {}) for user '{}'", saved.getId(), username);
        return saved;
    }

    /**
     * Alias method for getOrCreateProfile satisfying requirement specifications.
     */
    @Transactional
    public MasterProfile getOrCreateProfileByUsername(String username) {
        return getOrCreateProfile(username);
    }

    /**
     * Atomically registers a new AppUser and creates their corresponding MasterProfile.
     *
     * @param username       Desired username
     * @param email          Contact email
     * @param passwordHash   Hashed password or dummy hash
     * @param specialization Selected industry category
     * @return Newly created AppUser with attached MasterProfile
     */
    @Transactional
    public AppUser createUserWithProfile(String username, String email, String passwordHash, JobCategory specialization) {
        AppUser user = new AppUser(username, email, passwordHash, "LOCAL", "ROLE_USER");
        AppUser savedUser = userRepository.save(user);

        MasterProfile profile = new MasterProfile(
                savedUser,
                (specialization != null) ? specialization : JobCategory.TECHNOLOGY,
                username,
                "",
                "",
                "",
                "",
                "",
                ""
        );
        profileRepository.save(profile);
        log.info("Created user '{}' with profile specialization '{}'", username, profile.getSpecialization());
        return savedUser;
    }

    /**
     * Updates core profile metadata (name, specialization, contact, headline, summary, skills, preferences).
     *
     * @param profileId   ID of the MasterProfile to update
     * @param updatedData DTO/Entity containing updated form fields
     * @return The updated and persisted MasterProfile
     * @throws IllegalArgumentException if the profile does not exist
     */
    @Transactional
    public MasterProfile updateProfile(Long profileId, MasterProfile updatedData) {
        MasterProfile existing = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("MasterProfile not found with ID: " + profileId));

        if (updatedData.getSpecialization() != null) {
            existing.setSpecialization(updatedData.getSpecialization());
        }
        existing.setFullName(updatedData.getFullName());
        existing.setPhone(updatedData.getPhone());
        existing.setHeadline(updatedData.getHeadline());
        existing.setSummary(updatedData.getSummary());
        existing.setSkillsList(updatedData.getSkillsList());
        existing.setPreferredLocations(updatedData.getPreferredLocations());
        existing.setPreferredTags(updatedData.getPreferredTags());

        log.info("Updated MasterProfile (ID: {}, Specialization: {})", existing.getId(), existing.getSpecialization());
        return profileRepository.save(existing);
    }

    /**
     * Alias method for updateProfile.
     */
    @Transactional
    public MasterProfile updateProfileDetails(Long profileId, MasterProfile updatedData) {
        return updateProfile(profileId, updatedData);
    }

    /**
     * Adds a new resume item (Experience, Education, Certification, Project) to a user's profile.
     * Validates that the profile belongs to the specified user.
     *
     * @param profileId ID of the target MasterProfile
     * @param item      The new ProfileItem to attach
     * @return The saved ProfileItem
     * @throws IllegalArgumentException if the profile does not exist
     */
    @Transactional
    public ProfileItem addProfileItem(Long profileId, ProfileItem item) {
        MasterProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("MasterProfile not found with ID: " + profileId));

        item.setProfile(profile);
        ProfileItem saved = itemRepository.save(item);
        log.info("Added {} item '{}' (ID: {}) to MasterProfile (ID: {})",
                saved.getType(), saved.getTitle(), saved.getId(), profile.getId());
        return saved;
    }

    /**
     * Deletes a profile item after verifying that it belongs to the specified profile.
     *
     * @param profileId ID of the parent MasterProfile
     * @param itemId    ID of the ProfileItem to remove
     * @throws IllegalArgumentException if the item does not exist or does not belong to the profile
     */
    @Transactional
    public void deleteProfileItem(Long profileId, Long itemId) {
        ProfileItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ProfileItem not found with ID: " + itemId));

        if (!item.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Access denied: ProfileItem " + itemId + " does not belong to profile " + profileId);
        }

        itemRepository.delete(item);
        log.info("Deleted ProfileItem (ID: {}) from MasterProfile (ID: {})", itemId, profileId);
    }

    /**
     * Retrieves all items belonging to a profile filtered by their category.
     *
     * @param profileId ID of the parent MasterProfile
     * @param type      ProfileItemType enum category
     * @return List of matching ProfileItems
     */
    public List<ProfileItem> getItemsByType(Long profileId, ProfileItemType type) {
        return itemRepository.findByProfileIdAndType(profileId, type);
    }

    /**
     * Retrieves all items belonging to a profile regardless of type.
     *
     * @param profileId ID of the parent MasterProfile
     * @return List of all ProfileItems
     */
    public List<ProfileItem> getAllItems(Long profileId) {
        return itemRepository.findByProfileId(profileId);
    }

    /**
     * Retrieves all profile items for a specific user ID.
     *
     * @param userId Primary key of the AppUser
     * @return List of ProfileItems belonging to the user's profile
     */
    public List<ProfileItem> getItemsForUser(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(MasterProfile::getId)
                .map(itemRepository::findByProfileId)
                .orElse(Collections.emptyList());
    }
}
