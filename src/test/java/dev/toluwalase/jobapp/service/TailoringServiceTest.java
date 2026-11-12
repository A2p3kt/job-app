package dev.toluwalase.jobapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit test suite for TailoringService.
 * Validates deterministic offline fallback assembling, existing kit caching,
 * resilient zero-crash behavior, and draft updating.
 */
@ExtendWith(MockitoExtension.class)
public class TailoringServiceTest {

    @Mock
    private ApplicationKitRepository kitRepository;

    @Mock
    private JobPostingRepository jobRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MasterProfileRepository profileRepository;

    @Mock
    private ProfileItemRepository itemRepository;

    @Mock
    private MatchingEngineService matchingEngineService;

    private TailoringService tailoringService;

    private AppUser testUser;
    private MasterProfile testProfile;
    private JobPosting testJob;
    private List<ProfileItem> testItems;
    private JobMatchDto testMatch;

    @BeforeEach
    void setUp() {
        // Initialize service with no API key (offline deterministic mode)
        tailoringService = new TailoringService(
                kitRepository,
                jobRepository,
                userRepository,
                profileRepository,
                itemRepository,
                matchingEngineService,
                null,
                new ObjectMapper(),
                ""
        );

        testUser = new AppUser("tolu", "tolu@example.com", "hash", "LOCAL", "ROLE_USER");
        testUser.setId(1L);

        testProfile = new MasterProfile(
                testUser,
                JobCategory.TECHNOLOGY,
                "Toluwalase Ayoola",
                "+358 40 123 4567",
                "Senior Backend Engineer",
                "Passionate software engineer building resilient distributed systems.",
                "Java, Spring Boot, Docker, SQL, Kubernetes",
                "Helsinki, Remote",
                "Backend, Cloud"
        );
        testProfile.setId(10L);

        testJob = new JobPosting(
                "jobtech-101",
                JobCategory.TECHNOLOGY,
                "Senior Java Developer",
                "Nordic Tech Solutions",
                "Helsinki",
                "Looking for a Senior Java Developer with Spring Boot and Docker expertise.",
                "Java, Spring Boot, Docker",
                "https://jobtechdev.se/jobs/101",
                "JobTech Nordics",
                LocalDateTime.now()
        );
        testJob.setId(100L);

        ProfileItem expItem = new ProfileItem(
                testProfile,
                ProfileItemType.EXPERIENCE,
                "Lead Backend Engineer",
                "Nordic Software Oy",
                "Helsinki, Finland",
                "2022-01",
                "Present",
                "Architected high-throughput microservices using Spring Boot and Java."
        );
        expItem.setId(201L);

        ProfileItem eduItem = new ProfileItem(
                testProfile,
                ProfileItemType.EDUCATION,
                "M.Sc. Computer Science",
                "Aalto University",
                "Espoo, Finland",
                "2018",
                "2020",
                "Specialization in Software Engineering and Distributed Computing."
        );
        eduItem.setId(202L);

        ProfileItem projItem = new ProfileItem(
                testProfile,
                ProfileItemType.PROJECT,
                "Cloud-Native E-Commerce Platform",
                "Personal Portfolio",
                "Remote",
                "2023",
                "2023",
                "Engineered event-driven architecture using Kafka, Docker, and Spring Cloud."
        );
        projItem.setId(203L);

        testItems = Arrays.asList(expItem, eduItem, projItem);
        testMatch = new JobMatchDto(testJob, 92, 38, 54, Arrays.asList("Specialization Match (Technology)", "Skill: Java", "Skill: Spring Boot"));
    }

    @Test
    @DisplayName("generateDeterministicCv generates structured, ATS-friendly markdown with all profile sections")
    void testGenerateDeterministicCv() {
        String cv = tailoringService.generateDeterministicCv(testProfile, testItems, testJob, testMatch);

        assertNotNull(cv);
        assertTrue(cv.contains("# Toluwalase Ayoola"), "CV should contain candidate's full name header");
        assertTrue(cv.contains("tolu@example.com"), "CV should contain candidate's contact email");
        assertTrue(cv.contains("Senior Java Developer at Nordic Tech Solutions"), "CV should cite target position and employer");
        assertTrue(cv.contains("## PROFESSIONAL SUMMARY"), "CV should include Professional Summary section");
        assertTrue(cv.contains("Passionate software engineer building resilient distributed systems."), "CV should include candidate bio");
        assertTrue(cv.contains("## CORE COMPETENCIES"), "CV should include Core Competencies section");
        assertTrue(cv.contains("Java"), "CV should list technical skills");
        assertTrue(cv.contains("Spring Boot"), "CV should list technical skills");
        assertTrue(cv.contains("## PROFESSIONAL EXPERIENCE"), "CV should include Professional Experience section");
        assertTrue(cv.contains("Lead Backend Engineer \u2014 Nordic Software Oy"), "CV should include experience items");
        assertTrue(cv.contains("## EDUCATION & CREDENTIALS"), "CV should include Education section");
        assertTrue(cv.contains("Aalto University"), "CV should include education details");
        assertTrue(cv.contains("## KEY PROJECTS"), "CV should include Projects section");
        assertTrue(cv.contains("Cloud-Native E-Commerce Platform"), "CV should include project details");
    }

    @Test
    @DisplayName("generateDeterministicCoverLetter generates formal business correspondence aligned with target job")
    void testGenerateDeterministicCoverLetter() {
        String letter = tailoringService.generateDeterministicCoverLetter(testProfile, testItems, testJob, testMatch);

        assertNotNull(letter);
        assertTrue(letter.contains("Nordic Tech Solutions"), "Cover letter should reference the target employer");
        assertTrue(letter.contains("Senior Java Developer"), "Cover letter should reference the targeted role");
        assertTrue(letter.contains("Dear Hiring Team at Nordic Tech Solutions"), "Cover letter should include formal salutation");
        assertTrue(letter.contains("Technology & IT"), "Cover letter should cite candidate specialization");
        assertTrue(letter.contains("92% compatibility score"), "Cover letter should mention match compatibility");
        assertTrue(letter.contains("Sincerely,"), "Cover letter should include formal sign-off");
        assertTrue(letter.contains("Toluwalase Ayoola"), "Cover letter should include candidate's full name");
    }

    @Test
    @DisplayName("generateOrGetKit returns cached ApplicationKit if one already exists for user and job")
    void testGenerateOrGetKitReturnsExistingKit() {
        ApplicationKit existingKit = new ApplicationKit(
                testUser,
                testJob,
                92,
                "Existing rationale",
                "Existing CV",
                "Existing Letter",
                "DRAFT"
        );
        existingKit.setId(500L);

        when(kitRepository.findByUserIdAndJobPostingId(1L, 100L)).thenReturn(Optional.of(existingKit));

        ApplicationKit result = tailoringService.generateOrGetKit(1L, 100L);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("Existing CV", result.getTailoredCvContent());
        verify(kitRepository, never()).save(any(ApplicationKit.class));
    }

    @Test
    @DisplayName("generateOrGetKit generates, persists and returns a new kit when no existing kit is found")
    void testGenerateOrGetKitCreatesNewKit() {
        when(kitRepository.findByUserIdAndJobPostingId(1L, 100L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(itemRepository.findByProfileId(10L)).thenReturn(testItems);
        when(matchingEngineService.scoreJob(testProfile, testJob)).thenReturn(testMatch);

        when(kitRepository.save(any(ApplicationKit.class))).thenAnswer(invocation -> {
            ApplicationKit kit = invocation.getArgument(0);
            kit.setId(999L);
            return kit;
        });

        ApplicationKit result = tailoringService.generateOrGetKit(1L, 100L);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertEquals(92, result.getMatchScore());
        assertEquals("DRAFT", result.getStatus());
        assertTrue(result.getTailoredCvContent().contains("Toluwalase Ayoola"));
        assertTrue(result.getTailoredCoverLetterContent().contains("Nordic Tech Solutions"));
        verify(kitRepository).save(any(ApplicationKit.class));
    }

    @Test
    @DisplayName("generateOrGetKit throws IllegalArgumentException when job posting or user does not exist")
    void testGenerateOrGetKitThrowsWhenNotFound() {
        when(kitRepository.findByUserIdAndJobPostingId(1L, 999L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            tailoringService.generateOrGetKit(1L, 999L);
        });
    }

    @Test
    @DisplayName("updateKit successfully persists edited CV and Cover Letter text")
    void testUpdateKit() {
        ApplicationKit existingKit = new ApplicationKit(
                testUser,
                testJob,
                85,
                "Good match",
                "Original CV",
                "Original Letter",
                "DRAFT"
        );
        existingKit.setId(777L);

        when(kitRepository.findById(777L)).thenReturn(Optional.of(existingKit));
        when(kitRepository.save(any(ApplicationKit.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        ApplicationKit updated = tailoringService.updateKit(777L, "Customized CV Content", "Customized Letter Content");

        assertNotNull(updated);
        assertEquals("Customized CV Content", updated.getTailoredCvContent());
        assertEquals("Customized Letter Content", updated.getTailoredCoverLetterContent());
        verify(kitRepository).save(existingKit);
    }
}

