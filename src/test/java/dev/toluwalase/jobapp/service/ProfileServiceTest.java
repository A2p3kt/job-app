package dev.toluwalase.jobapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Unit test suite for ProfileService.
 * Validates profile retrieval, default profile initialization, user creation
 * with profile,
 * profile updating, item attachment, item deletion, and security ownership
 * checks.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private MasterProfileRepository profileRepository;

    @Mock
    private ProfileItemRepository itemRepository;

    @Mock
    private AppUserRepository userRepository;

    private ProfileService profileService;

    private AppUser testUser;
    private MasterProfile testProfile;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository, itemRepository, userRepository);

        testUser = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testUser.setId(1L);

        testProfile = new MasterProfile(
                testUser,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358401234567",
                "Senior Developer",
                "Software engineer.",
                "Java, Spring Boot",
                "Helsinki",
                "Backend");
        testProfile.setId(10L);
    }

    @Test
    @DisplayName("getProfileByUserId returns profile when found")
    void testGetProfileByUserId() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        Optional<MasterProfile> found = profileService.getProfileByUserId(1L);
        assertTrue(found.isPresent());
        assertEquals("Toluwalase Ayoola", found.get().getFullName());
    }

    @Test
    @DisplayName("getProfileByUsername returns profile when found")
    void testGetProfileByUsername() {
        when(profileRepository.findByUserUsername("tolu")).thenReturn(Optional.of(testProfile));

        Optional<MasterProfile> found = profileService.getProfileByUsername("tolu");
        assertTrue(found.isPresent());
        assertEquals("Toluwalase Ayoola", found.get().getFullName());
    }

    @Test
    @DisplayName("getOrCreateProfile returns existing profile if one exists")
    void testGetOrCreateProfileExisting() {
        when(profileRepository.findByUserUsername("tolu")).thenReturn(Optional.of(testProfile));

        MasterProfile result = profileService.getOrCreateProfile("tolu");
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    @DisplayName("getOrCreateProfile initializes new profile when user exists but has no profile")
    void testGetOrCreateProfileInitializesNew() {
        when(profileRepository.findByUserUsername("tolu")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("tolu")).thenReturn(Optional.of(testUser));
        when(profileRepository.save(any(MasterProfile.class))).thenAnswer(invocation -> {
            MasterProfile p = invocation.getArgument(0);
            p.setId(99L);
            return p;
        });

        MasterProfile result = profileService.getOrCreateProfile("tolu");
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("tolu", result.getFullName());
        verify(profileRepository).save(any(MasterProfile.class));
    }

    @Test
    @DisplayName("getOrCreateProfile throws IllegalArgumentException when username does not exist")
    void testGetOrCreateProfileThrowsWhenUserNotFound() {
        when(profileRepository.findByUserUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.getOrCreateProfile("unknown"));
    }

    @Test
    @DisplayName("createUserWithProfile registers user and creates profile")
    void testCreateUserWithProfile() {
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser u = invocation.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(profileRepository.save(any(MasterProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser created = profileService.createUserWithProfile("newuser", "new@example.com", "hash",
                JobCategory.HEALTHCARE);
        assertNotNull(created);
        assertEquals(5L, created.getId());
        verify(profileRepository).save(any(MasterProfile.class));
    }

    @Test
    @DisplayName("updateProfile modifies profile fields and persists changes")
    void testUpdateProfile() {
        when(profileRepository.findById(10L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(MasterProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MasterProfile updateData = new MasterProfile();
        updateData.setFullName("Updated Name");
        updateData.setSpecialization(JobCategory.MARKETING);
        updateData.setHeadline("Marketing Director");
        updateData.setPhone("+358 50 999 8888");
        updateData.setSummary("New summary");
        updateData.setSkillsList("SEO, SEM");
        updateData.setPreferredLocations("Tampere");
        updateData.setPreferredTags("Growth");

        MasterProfile updated = profileService.updateProfile(10L, updateData);
        assertEquals("Updated Name", updated.getFullName());
        assertEquals(JobCategory.MARKETING, updated.getSpecialization());
        assertEquals("Marketing Director", updated.getHeadline());
        assertEquals("SEO, SEM", updated.getSkillsList());
        verify(profileRepository).save(testProfile);
    }

    @Test
    @DisplayName("updateProfile throws IllegalArgumentException when profile ID not found")
    void testUpdateProfileNotFound() {
        when(profileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile(999L, new MasterProfile()));
    }

    @Test
    @DisplayName("addProfileItem associates item with profile and saves")
    void testAddProfileItem() {
        when(profileRepository.findById(10L)).thenReturn(Optional.of(testProfile));
        when(itemRepository.save(any(ProfileItem.class))).thenAnswer(invocation -> {
            ProfileItem item = invocation.getArgument(0);
            item.setId(50L);
            return item;
        });

        ProfileItem newItem = new ProfileItem(null, ProfileItemType.EXPERIENCE, "Dev", "Co", "Helsinki", "2023", "2024",
                "Work");
        ProfileItem saved = profileService.addProfileItem(10L, newItem);

        assertNotNull(saved);
        assertEquals(50L, saved.getId());
        assertEquals(testProfile, saved.getProfile());
        verify(itemRepository).save(newItem);
    }

    @Test
    @DisplayName("deleteProfileItem successfully removes item when ownership matches")
    void testDeleteProfileItemSuccess() {
        ProfileItem item = new ProfileItem(testProfile, ProfileItemType.EDUCATION, "B.Sc.", "Uni", "Espoo", "2018",
                "2022", "Studies");
        item.setId(25L);

        when(itemRepository.findById(25L)).thenReturn(Optional.of(item));

        profileService.deleteProfileItem(10L, 25L);
        verify(itemRepository).delete(item);
    }

    @Test
    @DisplayName("deleteProfileItem throws IllegalArgumentException when item belongs to another profile")
    void testDeleteProfileItemOwnershipMismatch() {
        MasterProfile otherProfile = new MasterProfile();
        otherProfile.setId(99L);

        ProfileItem item = new ProfileItem(otherProfile, ProfileItemType.EDUCATION, "B.Sc.", "Uni", "Espoo", "2018",
                "2022", "Studies");
        item.setId(25L);

        when(itemRepository.findById(25L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> profileService.deleteProfileItem(10L, 25L));
    }

    @Test
    @DisplayName("deleteProfileItem throws IllegalArgumentException when item does not exist")
    void testDeleteProfileItemNotFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.deleteProfileItem(10L, 999L));
    }

    @Test
    @DisplayName("getItemsByType queries itemRepository by profile and type")
    void testGetItemsByType() {
        ProfileItem item = new ProfileItem(testProfile, ProfileItemType.CERTIFICATION, "AWS", "Amazon", "Remote",
                "2023", "2026", "Cloud");
        when(itemRepository.findByProfileIdAndType(10L, ProfileItemType.CERTIFICATION)).thenReturn(List.of(item));

        List<ProfileItem> items = profileService.getItemsByType(10L, ProfileItemType.CERTIFICATION);
        assertEquals(1, items.size());
        assertEquals("AWS", items.get(0).getTitle());
    }

    @Test
    @DisplayName("getAllItems queries itemRepository for all profile items")
    void testGetAllItems() {
        ProfileItem item = new ProfileItem(testProfile, ProfileItemType.PROJECT, "App", "Self", "Remote", "2024",
                "2024", "Demo");
        when(itemRepository.findByProfileId(10L)).thenReturn(List.of(item));

        List<ProfileItem> items = profileService.getAllItems(10L);
        assertEquals(1, items.size());
        assertEquals("App", items.get(0).getTitle());
    }

    @Test
    @DisplayName("getItemsByType returns empty list on null profile ID")
    void testGetItemsByTypeNullSafety() {
        List<ProfileItem> items = profileService.getItemsByType(null, ProfileItemType.EXPERIENCE);
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("getAllItems returns empty list on null profile ID")
    void testGetAllItemsNullSafety() {
        List<ProfileItem> items = profileService.getAllItems(null);
        assertTrue(items.isEmpty());
    }
}
