package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import dev.toluwalase.jobapp.service.ProfileService;

/**
 * Slice unit test suite for ProfileWebController.
 * Validates PRG redirects, view names, model attributes, and validation handling.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileWebControllerTest {

    @Mock
    private ProfileService profileService;

    private MockMvc mockMvc;
    private MasterProfile testProfile;

    @BeforeEach
    void setUp() {
        ProfileWebController controller = new ProfileWebController(profileService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        AppUser user = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testProfile = new MasterProfile(
                user,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358 40 123 4567",
                "Senior Software Engineer",
                "Bio",
                "Java, Spring Boot",
                "Helsinki",
                "Backend"
        );
        testProfile.setId(10L);
    }

    @Test
    @DisplayName("GET / redirects to /profile")
    void testIndexRedirectsToProfile() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @DisplayName("GET /profile renders profile view with bound model attributes")
    void testShowProfile() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(profileService.getItemsByType(10L, ProfileItemType.EXPERIENCE)).thenReturn(List.of());
        when(profileService.getItemsByType(10L, ProfileItemType.EDUCATION)).thenReturn(List.of());
        when(profileService.getItemsByType(10L, ProfileItemType.CERTIFICATION)).thenReturn(List.of());
        when(profileService.getItemsByType(10L, ProfileItemType.PROJECT)).thenReturn(List.of());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("profile", "categories", "itemTypes", "newItem",
                        "experienceItems", "educationItems", "certificationItems", "projectItems"));
    }

    @Test
    @DisplayName("POST /profile/update updates profile and redirects with flash message")
    void testUpdateProfileSuccess() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .param("id", "10")
                        .param("fullName", "Toluwalase Ayoola")
                        .param("specialization", "TECHNOLOGY")
                        .param("headline", "Dev")
                        .param("summary", "Bio")
                        .param("skillsList", "Java, Spring Boot")
                        .param("preferredLocations", "Helsinki")
                        .param("preferredTags", "Backend"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).updateProfile(eq(10L), any(MasterProfile.class));
    }

    @Test
    @DisplayName("POST /profile/update re-renders profile view on validation failure")
    void testUpdateProfileValidationFailure() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);

        mockMvc.perform(post("/profile/update")
                        .param("id", "10")
                        .param("fullName", "") // Invalid: @NotBlank
                        .param("specialization", "TECHNOLOGY"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /profile/items/add attaches item and redirects with success flash")
    void testAddProfileItemSuccess() throws Exception {
        mockMvc.perform(post("/profile/items/add")
                        .param("profileId", "10")
                        .param("type", "EXPERIENCE")
                        .param("title", "Software Engineer")
                        .param("organization", "Acme")
                        .param("location", "Helsinki")
                        .param("startDate", "2023-01")
                        .param("endDate", "2024-01")
                        .param("description", "Building apps"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).addProfileItem(eq(10L), any(ProfileItem.class));
    }

    @Test
    @DisplayName("POST /profile/items/add redirects with error flash on validation failure")
    void testAddProfileItemValidationFailure() throws Exception {
        mockMvc.perform(post("/profile/items/add")
                        .param("profileId", "10")
                        .param("title", "")) // Invalid: missing type and blank title
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /profile/items/{id}/delete deletes item and redirects")
    void testDeleteProfileItemSuccess() throws Exception {
        mockMvc.perform(post("/profile/items/55/delete")
                        .param("profileId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).deleteProfileItem(10L, 55L);
    }

    @Test
    @DisplayName("POST /profile/items/{id}/delete redirects with error flash on failure")
    void testDeleteProfileItemThrowsException() throws Exception {
        doThrow(new IllegalArgumentException("Item does not belong to profile"))
                .when(profileService).deleteProfileItem(10L, 99L);

        mockMvc.perform(post("/profile/items/99/delete")
                        .param("profileId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
