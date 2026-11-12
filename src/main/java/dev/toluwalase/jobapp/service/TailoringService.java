package dev.toluwalase.jobapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.JobMatchDto;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Service orchestrating AI-powered tailoring of job applications.
 * Produces structured, customized CVs and compelling cover letters for a specific
 * (Candidate, JobPosting) pair.
 *
 * Resilience Guarantee:
 * When an active Gemini API key is provided and reachable, it uses LLM inference.
 * When the key is absent, blank, or the external API call fails, it automatically
 * falls back to a deterministic, profile-driven template generator, guaranteeing
 * zero crashes and 100% offline testability.
 */
@Service
@Transactional
public class TailoringService {

    private static final Logger log = LoggerFactory.getLogger(TailoringService.class);

    private final ApplicationKitRepository kitRepository;
    private final JobPostingRepository jobRepository;
    private final AppUserRepository userRepository;
    private final MasterProfileRepository profileRepository;
    private final ProfileItemRepository itemRepository;
    private final MatchingEngineService matchingEngineService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;

    /**
     * Explicit constructor injection without Lombok.
     */
    public TailoringService(ApplicationKitRepository kitRepository,
                            JobPostingRepository jobRepository,
                            AppUserRepository userRepository,
                            MasterProfileRepository profileRepository,
                            ProfileItemRepository itemRepository,
                            MatchingEngineService matchingEngineService,
                            @Autowired(required = false) RestClient.Builder restClientBuilder,
                            @Autowired(required = false) ObjectMapper objectMapper,
                            @Value("${gemini.api.key:}") String geminiApiKey) {
        this.kitRepository = kitRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.itemRepository = itemRepository;
        this.matchingEngineService = matchingEngineService;
        this.restClient = (restClientBuilder != null) ? restClientBuilder.build() : RestClient.create();
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.geminiApiKey = (geminiApiKey != null) ? geminiApiKey.trim() : "";
    }

    /**
     * Generates or retrieves an existing ApplicationKit for a given candidate user ID and job posting ID.
     *
     * @param userId Primary key of the candidate AppUser
     * @param jobId  Primary key of the target JobPosting
     * @return Generated or existing ApplicationKit entity
     */
    public ApplicationKit generateOrGetKit(Long userId, Long jobId) {
        Optional<ApplicationKit> existing = kitRepository.findByUserIdAndJobPostingId(userId, jobId);
        if (existing.isPresent()) {
            log.info("Found existing ApplicationKit (ID: {}) for user ID {} and job ID {}",
                    existing.get().getId(), userId, jobId);
            return existing.get();
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found with ID: " + jobId));

        MasterProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Master profile not found for user ID: " + userId));

        List<ProfileItem> items = itemRepository.findByProfileId(profile.getId());
        JobMatchDto match = matchingEngineService.scoreJob(profile, job);

        String tailoredCv;
        String tailoredCoverLetter;

        if (!geminiApiKey.isEmpty()) {
            log.info("Gemini API key detected. Attempting live AI tailoring for job ID: {}", jobId);
            try {
                String[] aiGenerated = callGeminiAi(profile, items, job, match);
                tailoredCv = aiGenerated[0];
                tailoredCoverLetter = aiGenerated[1];
            } catch (Exception ex) {
                log.warn("Gemini API call failed ({}). Falling back to deterministic offline template generator.",
                        ex.getMessage());
                tailoredCv = generateDeterministicCv(profile, items, job, match);
                tailoredCoverLetter = generateDeterministicCoverLetter(profile, items, job, match);
            }
        } else {
            log.info("No Gemini API key configured. Utilizing deterministic offline template generator.");
            tailoredCv = generateDeterministicCv(profile, items, job, match);
            tailoredCoverLetter = generateDeterministicCoverLetter(profile, items, job, match);
        }

        ApplicationKit kit = new ApplicationKit(
                user,
                job,
                match.getMatchScore(),
                String.join(", ", match.getMatchReasons()),
                tailoredCv,
                tailoredCoverLetter,
                "DRAFT"
        );

        ApplicationKit savedKit = kitRepository.save(kit);
        log.info("Successfully persisted ApplicationKit (ID: {}) for user '{}' targeting '{}' at '{}'",
                savedKit.getId(), user.getUsername(), job.getTitle(), job.getCompany());
        return savedKit;
    }

    /**
     * Updates editable CV and Cover Letter text fields for a saved application kit.
     *
     * @param kitId Primary key of the ApplicationKit
     * @param cvContent Updated CV markdown/text
     * @param coverLetterContent Updated Cover Letter text
     * @return Updated ApplicationKit
     */
    public ApplicationKit updateKit(Long kitId, String cvContent, String coverLetterContent) {
        ApplicationKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationKit not found with ID: " + kitId));

        kit.setTailoredCvContent(cvContent);
        kit.setTailoredCoverLetterContent(coverLetterContent);
        kit.setUpdatedAt(LocalDateTime.now());

        log.info("Updated draft content for ApplicationKit (ID: {})", kitId);
        return kitRepository.save(kit);
    }

    /**
     * Retrieves an ApplicationKit by primary key.
     */
    @Transactional(readOnly = true)
    public Optional<ApplicationKit> getKitById(Long kitId) {
        return kitRepository.findById(kitId);
    }

    /**
     * Invokes the Google Gemini API to generate customized application materials.
     * Returns a 2-element array: [tailoredCv, tailoredCoverLetter].
     */
    private String[] callGeminiAi(MasterProfile profile, List<ProfileItem> items, JobPosting job, JobMatchDto match) throws Exception {
        String prompt = buildGeminiPrompt(profile, items, job, match);

        String requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        String requestBody = objectMapper.writeValueAsString(Collections.singletonMap("contents",
                Collections.singletonList(Collections.singletonMap("parts",
                        Collections.singletonList(Collections.singletonMap("text", prompt))))));

        String rawResponse = restClient.post()
                .uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini API response did not contain candidates");
        }

        String outputText = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        if (outputText.isBlank()) {
            throw new IllegalStateException("Empty text content received from Gemini API");
        }

        String delimiter = "===COVER_LETTER===";
        if (outputText.contains(delimiter)) {
            String[] parts = outputText.split(delimiter, 2);
            return new String[]{parts[0].trim(), parts[1].trim()};
        } else {
            // Fallback: entire text as CV, generate deterministic cover letter
            return new String[]{outputText.trim(), generateDeterministicCoverLetter(profile, items, job, match)};
        }
    }

    /**
     * Constructs the structured prompt for Gemini LLM.
     */
    private String buildGeminiPrompt(MasterProfile profile, List<ProfileItem> items, JobPosting job, JobMatchDto match) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert career consultant and professional resume writer specializing in the ")
                .append(profile.getSpecialization().getDisplayName())
                .append(" industry.\n\n");

        sb.append("Generate two tailored documents for the candidate targeting the job opening described below:\n");
        sb.append("1. A tailored, ATS-friendly CV / Resume in clean Markdown format.\n");
        sb.append("2. A compelling, tailored Cover Letter in formal business correspondence format.\n\n");
        sb.append("Separate the two documents EXACTLY with the delimiter '===COVER_LETTER===' on its own line.\n\n");

        sb.append("### CANDIDATE PROFILE:\n");
        sb.append("Full Name: ").append(profile.getFullName()).append("\n");
        sb.append("Headline: ").append(profile.getHeadline()).append("\n");
        sb.append("Email: ").append(profile.getUser() != null ? profile.getUser().getEmail() : "candidate@example.com").append("\n");
        sb.append("Phone: ").append(profile.getPhone()).append("\n");
        sb.append("Industry Specialization: ").append(profile.getSpecialization().getDisplayName()).append("\n");
        sb.append("Summary: ").append(profile.getSummary()).append("\n");
        sb.append("Skills: ").append(profile.getSkillsList()).append("\n");
        sb.append("Preferred Locations: ").append(profile.getPreferredLocations()).append("\n");

        sb.append("\nCandidate Profile Items:\n");
        for (ProfileItem item : items) {
            sb.append("- [").append(item.getType()).append("] ")
                    .append(item.getTitle()).append(" at ").append(item.getOrganization())
                    .append(" (").append(item.getStartDate()).append(" - ")
                    .append(item.getEndDate() != null ? item.getEndDate() : "Present").append("): ")
                    .append(item.getDescription()).append("\n");
        }

        sb.append("\n### TARGET JOB OPENING:\n");
        sb.append("Title: ").append(job.getTitle()).append("\n");
        sb.append("Employer: ").append(job.getCompany()).append("\n");
        sb.append("Location: ").append(job.getLocation()).append("\n");
        sb.append("Category: ").append(job.getCategory().getDisplayName()).append("\n");
        sb.append("Indexed Tags / Requirements: ").append(job.getExtractedTags()).append("\n");
        sb.append("Compatibility Fit Score: ").append(match.getMatchScore()).append("%\n");
        sb.append("Match Rationale: ").append(String.join(", ", match.getMatchReasons())).append("\n");
        sb.append("Description:\n").append(job.getDescription()).append("\n");

        return sb.toString();
    }

    /**
     * Deterministic, zero-crash CV generator assembling structured markdown
     * directly from the candidate's real profile items and target job requirements.
     */
    public String generateDeterministicCv(MasterProfile profile, List<ProfileItem> items, JobPosting job, JobMatchDto match) {
        StringBuilder sb = new StringBuilder();

        String email = (profile.getUser() != null && profile.getUser().getEmail() != null)
                ? profile.getUser().getEmail()
                : "candidate@example.com";
        String phone = (profile.getPhone() != null && !profile.getPhone().isBlank())
                ? profile.getPhone()
                : "+1 555-0199";
        String location = (profile.getPreferredLocations() != null && !profile.getPreferredLocations().isBlank())
                ? profile.getPreferredLocations()
                : (job.getLocation() != null ? job.getLocation() : "Remote");

        // Header
        sb.append("# ").append(profile.getFullName()).append("\n");
        sb.append("**").append(profile.getHeadline()).append("** | ")
                .append(email).append(" | ")
                .append(phone).append(" | ")
                .append(location).append("\n\n");

        sb.append("> **Target Position:** ").append(job.getTitle())
                .append(" at ").append(job.getCompany())
                .append(" (Compatibility Fit: ").append(match.getMatchScore()).append("%)\n\n");

        // Professional Summary
        sb.append("## PROFESSIONAL SUMMARY\n");
        if (profile.getSummary() != null && !profile.getSummary().isBlank()) {
            sb.append(profile.getSummary().trim()).append("\n\n");
        }
        sb.append("Results-oriented ").append(profile.getSpecialization().getDisplayName())
                .append(" professional offering dedicated expertise aligned with ").append(job.getCompany())
                .append("'s mission. Proven aptitude in applying core capabilities (")
                .append(job.getExtractedTags() != null ? job.getExtractedTags() : "industry standards")
                .append(") to drive operational excellence, cross-functional collaboration, and measurable outcomes.\n\n");

        // Core Competencies
        sb.append("## CORE COMPETENCIES & MATCHED EXPERTISE\n");
        if (profile.getSkillsList() != null && !profile.getSkillsList().isBlank()) {
            String[] skills = profile.getSkillsList().split(",");
            for (String skill : skills) {
                if (!skill.trim().isEmpty()) {
                    sb.append("- ").append(skill.trim()).append("\n");
                }
            }
        }
        if (job.getExtractedTags() != null && !job.getExtractedTags().isBlank()) {
            sb.append("- Key Role Competencies: ").append(job.getExtractedTags()).append("\n");
        }
        sb.append("\n");

        // Professional Experience
        List<ProfileItem> experiences = items.stream()
                .filter(i -> i.getType() == ProfileItemType.EXPERIENCE)
                .collect(Collectors.toList());

        sb.append("## PROFESSIONAL EXPERIENCE\n");
        if (!experiences.isEmpty()) {
            for (ProfileItem exp : experiences) {
                sb.append("### ").append(exp.getTitle()).append(" \u2014 ").append(exp.getOrganization()).append("\n");
                String start = (exp.getStartDate() != null && !exp.getStartDate().isBlank()) ? exp.getStartDate() : "2022";
                String end = (exp.getEndDate() != null && !exp.getEndDate().isBlank()) ? exp.getEndDate() : "Present";
                sb.append("*").append(start).append(" \u2013 ").append(end).append("*\n\n");
                if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                    sb.append(exp.getDescription().trim()).append("\n\n");
                }
            }
        } else {
            sb.append("### ").append(profile.getHeadline()).append(" \u2014 Industry Experience\n");
            sb.append("*2022 – Present*\n\n");
            sb.append("Demonstrated proven success in the ").append(profile.getSpecialization().getDisplayName())
                    .append(" field, consistently executing key deliverables and adhering to best practices.\n\n");
        }

        // Education & Credentials
        List<ProfileItem> education = items.stream()
                .filter(i -> i.getType() == ProfileItemType.EDUCATION || i.getType() == ProfileItemType.CERTIFICATION)
                .collect(Collectors.toList());

        if (!education.isEmpty()) {
            sb.append("## EDUCATION & CREDENTIALS\n");
            for (ProfileItem edu : education) {
                sb.append("### ").append(edu.getTitle()).append(" \u2014 ").append(edu.getOrganization()).append("\n");
                if (edu.getStartDate() != null && !edu.getStartDate().isBlank()) {
                    sb.append("*").append(edu.getStartDate())
                            .append(edu.getEndDate() != null ? " \u2013 " + edu.getEndDate() : "")
                            .append("*\n\n");
                }
                if (edu.getDescription() != null && !edu.getDescription().isBlank()) {
                    sb.append(edu.getDescription().trim()).append("\n\n");
                }
            }
        }

        // Key Projects
        List<ProfileItem> projects = items.stream()
                .filter(i -> i.getType() == ProfileItemType.PROJECT)
                .collect(Collectors.toList());

        if (!projects.isEmpty()) {
            sb.append("## KEY PROJECTS & ACCOMPLISHMENTS\n");
            for (ProfileItem proj : projects) {
                sb.append("### ").append(proj.getTitle()).append("\n");
                if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                    sb.append(proj.getDescription().trim()).append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Deterministic, zero-crash Cover Letter generator assembling an eloquent,
     * formal business letter directly from the candidate's real profile items.
     */
    public String generateDeterministicCoverLetter(MasterProfile profile, List<ProfileItem> items, JobPosting job, JobMatchDto match) {
        StringBuilder sb = new StringBuilder();

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String email = (profile.getUser() != null && profile.getUser().getEmail() != null)
                ? profile.getUser().getEmail()
                : "candidate@example.com";
        String phone = (profile.getPhone() != null && !profile.getPhone().isBlank())
                ? profile.getPhone()
                : "+1 555-0199";

        sb.append(dateStr).append("\n\n");
        sb.append("Hiring Team\n");
        sb.append(job.getCompany()).append("\n");
        if (job.getLocation() != null && !job.getLocation().isBlank()) {
            sb.append(job.getLocation()).append("\n");
        }
        sb.append("\n");

        sb.append("Dear Hiring Team at ").append(job.getCompany()).append(",\n\n");

        sb.append("I am writing to express my enthusiastic interest in the ")
                .append(job.getTitle()).append(" position currently available at ")
                .append(job.getCompany()).append(". Having established a solid foundation in ")
                .append(profile.getSpecialization().getDisplayName())
                .append(" and maintaining an active career focus as ")
                .append(profile.getHeadline())
                .append(", I am confident that my experience and skills make me a strong contender for this opportunity.\n\n");

        if (profile.getSummary() != null && !profile.getSummary().isBlank()) {
            sb.append(profile.getSummary().trim()).append("\n\n");
        }

        sb.append("Your posting highlights several key qualifications, including ")
                .append(job.getExtractedTags() != null ? job.getExtractedTags() : "industry-relevant skills")
                .append(". In my professional work, I have consistently demonstrated competence in these domains. ");

        Optional<ProfileItem> topExp = items.stream()
                .filter(i -> i.getType() == ProfileItemType.EXPERIENCE)
                .findFirst();

        if (topExp.isPresent()) {
            ProfileItem exp = topExp.get();
            sb.append("Notably, as ").append(exp.getTitle()).append(" at ").append(exp.getOrganization())
                    .append(", I contributed to meaningful initiatives by ")
                    .append(exp.getDescription() != null ? exp.getDescription().trim() : "delivering consistent operational value")
                    .append(". ");
        }

        sb.append("My composite qualifications achieved a ").append(match.getMatchScore())
                .append("% compatibility score with your requirements, backed by verified experience in: ")
                .append(String.join(", ", match.getMatchReasons())).append(".\n\n");

        sb.append("I am eager to bring my dedicated work ethic, collaborative spirit, and domain expertise to ")
                .append(job.getCompany()).append(". I welcome the opportunity to discuss in an interview how my capabilities can support your upcoming goals and team objectives.\n\n");

        sb.append("Thank you for your consideration. I look forward to speaking with you.\n\n");

        sb.append("Sincerely,\n\n");
        sb.append(profile.getFullName()).append("\n");
        sb.append(email).append(" | ").append(phone).append("\n");

        return sb.toString();
    }
}

