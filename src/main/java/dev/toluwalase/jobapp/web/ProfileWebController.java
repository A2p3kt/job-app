package dev.toluwalase.jobapp.web;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import dev.toluwalase.jobapp.service.ProfileService;
import jakarta.validation.Valid;

/**
 * Controller handling Thymeleaf web requests for viewing and managing candidate career profiles.
 * Implements the Post-Redirect-Get (PRG) pattern for all mutation operations to prevent duplicate submissions.
 */
@Controller
public class ProfileWebController {

    private static final Logger log = LoggerFactory.getLogger(ProfileWebController.class);

    private final ProfileService profileService;

    /**
     * Explicit constructor injection without Lombok.
     *
     * @param profileService Service layer handling profile business logic
     */
    public ProfileWebController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Redirects the root path to the profile view during development.
     *
     * @return Redirect to /profile
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/profile";
    }

    /**
     * Displays the Master Profile form, specialization selector, categorized items, and add-item modal.
     * Resolves the authenticated user, or falls back to the default seed user ("tolu") during development.
     *
     * @param model     Spring MVC Model for UI data binding
     * @param principal Current authenticated user principal (nullable prior to Milestone 6)
     * @return Template view name "profile"
     */
    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "tolu";
        MasterProfile profile = profileService.getOrCreateProfile(username);

        model.addAttribute("profile", profile);
        model.addAttribute("categories", JobCategory.values());
        model.addAttribute("itemTypes", ProfileItemType.values());
        model.addAttribute("newItem", new ProfileItem());

        // Group items by type for organized display
        model.addAttribute("experienceItems", profileService.getItemsByType(profile.getId(), ProfileItemType.EXPERIENCE));
        model.addAttribute("educationItems", profileService.getItemsByType(profile.getId(), ProfileItemType.EDUCATION));
        model.addAttribute("certificationItems", profileService.getItemsByType(profile.getId(), ProfileItemType.CERTIFICATION));
        model.addAttribute("projectItems", profileService.getItemsByType(profile.getId(), ProfileItemType.PROJECT));

        return "profile";
    }

    /**
     * Updates personal contact information, headline, summary, specialization, skills, and search preferences.
     *
     * @param profileForm        Bound form entity containing user edits
     * @param bindingResult      Validation result for profileForm
     * @param redirectAttributes Flash attributes for feedback notifications
     * @param model              Spring MVC Model for error rendering
     * @param principal          Current authenticated user
     * @return Redirect to /profile upon success or "profile" view on validation error
     */
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("profile") MasterProfile profileForm,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model,
                                Principal principal) {
        if (bindingResult.hasErrors()) {
            String username = (principal != null) ? principal.getName() : "tolu";
            MasterProfile existing = profileService.getOrCreateProfile(username);

            model.addAttribute("categories", JobCategory.values());
            model.addAttribute("itemTypes", ProfileItemType.values());
            model.addAttribute("newItem", new ProfileItem());
            model.addAttribute("experienceItems", profileService.getItemsByType(existing.getId(), ProfileItemType.EXPERIENCE));
            model.addAttribute("educationItems", profileService.getItemsByType(existing.getId(), ProfileItemType.EDUCATION));
            model.addAttribute("certificationItems", profileService.getItemsByType(existing.getId(), ProfileItemType.CERTIFICATION));
            model.addAttribute("projectItems", profileService.getItemsByType(existing.getId(), ProfileItemType.PROJECT));
            model.addAttribute("errorMessage", "Please correct the highlighted validation errors.");
            return "profile";
        }

        profileService.updateProfile(profileForm.getId(), profileForm);
        redirectAttributes.addFlashAttribute("successMessage", "Master Profile updated successfully!");
        return "redirect:/profile";
    }

    /**
     * Attaches a new resume item (Experience, Education, Certification, Project) to the profile.
     *
     * @param profileId          Parent MasterProfile ID
     * @param newItem            Bound form data for the new item
     * @param bindingResult      Validation result for newItem
     * @param redirectAttributes Flash attributes for feedback notifications
     * @return Redirect to /profile
     */
    @PostMapping("/profile/items/add")
    public String addProfileItem(@RequestParam("profileId") Long profileId,
                                 @Valid @ModelAttribute("newItem") ProfileItem newItem,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add item: Title and Type are required.");
            return "redirect:/profile";
        }

        profileService.addProfileItem(profileId, newItem);
        redirectAttributes.addFlashAttribute("successMessage", "Added new " + newItem.getType() + " item successfully!");
        return "redirect:/profile";
    }

    /**
     * Deletes a specific ProfileItem after validating parent profile ownership.
     *
     * @param itemId             ID of the ProfileItem to remove
     * @param profileId          Parent MasterProfile ID
     * @param redirectAttributes Flash attributes for feedback notifications
     * @return Redirect to /profile
     */
    @PostMapping("/profile/items/{id}/delete")
    public String deleteProfileItem(@PathVariable("id") Long itemId,
                                    @RequestParam("profileId") Long profileId,
                                    RedirectAttributes redirectAttributes) {
        try {
            profileService.deleteProfileItem(profileId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Profile item deleted successfully.");
        } catch (IllegalArgumentException ex) {
            log.error("Failed to delete item: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
