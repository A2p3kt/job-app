package dev.toluwalase.jobapp.web;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.service.ProfileService;
import jakarta.validation.Valid;

/**
 * RESTful API controller exposing endpoints for candidate MasterProfile and resume items.
 * Fulfills dual Web + REST architecture under /api/v1/profile.
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileApiController {

    private static final Logger log = LoggerFactory.getLogger(ProfileApiController.class);

    private final ProfileService profileService;

    public ProfileApiController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private String resolveUsername(Principal principal) {
        return (principal != null) ? principal.getName() : "tolu";
    }

    /**
     * Retrieves the candidate's MasterProfile for the authenticated user.
     *
     * @param principal Authenticated user security principal
     * @return 200 OK with MasterProfile JSON
     */
    @GetMapping
    public ResponseEntity<MasterProfile> getProfile(Principal principal) {
        String username = resolveUsername(principal);
        log.info("API request to fetch MasterProfile for user '{}'", username);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates core career dossier fields on the candidate's profile.
     *
     * @param updatedProfile Payload containing updated fields
     * @param principal      Authenticated user security principal
     * @return 200 OK with the updated MasterProfile
     */
    @PutMapping
    public ResponseEntity<MasterProfile> updateProfile(@RequestBody MasterProfile updatedProfile, Principal principal) {
        String username = resolveUsername(principal);
        log.info("API request to update MasterProfile for user '{}'", username);
        MasterProfile existing = profileService.getOrCreateProfile(username);
        MasterProfile saved = profileService.updateProfile(existing.getId(), updatedProfile);
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieves all modular resume items (experience, education, certifications, projects).
     *
     * @param principal Authenticated user security principal
     * @return List of ProfileItems
     */
    @GetMapping("/items")
    public ResponseEntity<List<ProfileItem>> getProfileItems(Principal principal) {
        String username = resolveUsername(principal);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        return ResponseEntity.ok(profileService.getAllItems(profile.getId()));
    }

    /**
     * Adds a new modular resume item to the candidate's profile.
     *
     * @param item      The ProfileItem to attach
     * @param principal Authenticated user security principal
     * @return 201 Created with the saved ProfileItem
     */
    @PostMapping("/items")
    public ResponseEntity<ProfileItem> addProfileItem(@Valid @RequestBody ProfileItem item, Principal principal) {
        String username = resolveUsername(principal);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        ProfileItem saved = profileService.addProfileItem(profile.getId(), item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Removes an existing resume item from the candidate's profile.
     *
     * @param itemId    ID of the item to delete
     * @param principal Authenticated user security principal
     * @return 204 No Content
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteProfileItem(@PathVariable Long itemId, Principal principal) {
        String username = resolveUsername(principal);
        MasterProfile profile = profileService.getOrCreateProfile(username);
        profileService.deleteProfileItem(profile.getId(), itemId);
        return ResponseEntity.noContent().build();
    }
}
