package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import dev.toluwalase.jobapp.service.ProfileService;
import tools.jackson.databind.ObjectMapper;

/**
 * Slice unit test suite for ProfileApiController.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProfileService profileService;

    private MasterProfile testProfile;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ProfileApiController controller = new ProfileApiController(profileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        AppUser user = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testProfile = new MasterProfile(
                user,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358401234567",
                "Full Stack Developer",
                "Software engineering professional.",
                "Java, Spring Boot, React",
                "Helsinki, Remote",
                "Developer, Backend"
        );
    }

    @Test
    @DisplayName("GET /api/v1/profile returns 200 OK with candidate profile JSON")
    void testGetProfile() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Toluwalase Ayoola"))
                .andExpect(jsonPath("$.specialization").value("TECHNOLOGY"))
                .andExpect(jsonPath("$.headline").value("Full Stack Developer"));
    }

    @Test
    @DisplayName("PUT /api/v1/profile updates and returns 200 OK with updated profile")
    void testUpdateProfile() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(profileService.updateProfile(any(), any(MasterProfile.class))).thenReturn(testProfile);

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Toluwalase Ayoola"));

        verify(profileService).updateProfile(any(), any(MasterProfile.class));
    }

    @Test
    @DisplayName("GET /api/v1/profile/items returns list of profile items")
    void testGetProfileItems() throws Exception {
        ProfileItem item = new ProfileItem(testProfile, ProfileItemType.EXPERIENCE, "Dev", "Acme", "Helsinki", "2023-01", "2024-01", "Duties");
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(profileService.getAllItems(any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/profile/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Dev"))
                .andExpect(jsonPath("$[0].type").value("EXPERIENCE"));
    }

    @Test
    @DisplayName("POST /api/v1/profile/items creates and returns 201 Created")
    void testAddProfileItem() throws Exception {
        ProfileItem item = new ProfileItem(testProfile, ProfileItemType.EDUCATION, "B.Sc.", "Uni", "Helsinki", "2020", "2024", "Studies");
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);
        when(profileService.addProfileItem(any(), any(ProfileItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/profile/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("B.Sc."));
    }

    @Test
    @DisplayName("DELETE /api/v1/profile/items/{id} removes item and returns 204 No Content")
    void testDeleteProfileItem() throws Exception {
        when(profileService.getOrCreateProfile("tolu")).thenReturn(testProfile);

        mockMvc.perform(delete("/api/v1/profile/items/55"))
                .andExpect(status().isNoContent());

        verify(profileService).deleteProfileItem(any(), eq(55L));
    }
}
