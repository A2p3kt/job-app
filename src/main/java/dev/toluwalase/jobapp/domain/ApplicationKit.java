package dev.toluwalase.jobapp.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Stores tailored application materials (customized CV and cover letter)
 * generated on-demand for a specific (AppUser, JobPosting) pair.
 */
@Entity
@Table(name = "application_kits")
public class ApplicationKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The candidate for whom this kit was produced.
     */
    @JsonIgnore
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * The specific job posting targeted by this kit.
     * Eagerly fetched so job details are readily available in kit views.
     */
    @NotNull(message = "Job posting is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting jobPosting;

    /**
     * Compatibility fit score (0 to 100) computed by MatchingEngineService.
     */
    @Column(name = "match_score")
    private Integer matchScore;

    /**
     * Explanation of matched skills, preferences, and qualifications.
     */
    @Column(name = "match_reasons", columnDefinition = "TEXT")
    private String matchReasons;

    /**
     * Editable, structured CV content tailored to this target job.
     */
    @Column(name = "tailored_cv_content", columnDefinition = "TEXT")
    private String tailoredCvContent;

    /**
     * Editable cover letter text customized for this role.
     */
    @Column(name = "tailored_cover_letter_content", columnDefinition = "TEXT")
    private String tailoredCoverLetterContent;

    /**
     * Current workflow state: "DRAFT", "FINALIZED", or "APPLIED".
     */
    @Column(nullable = false)
    private String status = "DRAFT";

    /**
     * Last modification timestamp.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Default no-argument constructor required by JPA.
     */
    public ApplicationKit() {
    }

    /**
     * Parameterized constructor for generating a new ApplicationKit without an ID.
     *
     * @param user                       Candidate owner
     * @param jobPosting                 Target job opening
     * @param matchScore                 Calculated match score (0-100)
     * @param matchReasons               Human-readable match explanation
     * @param tailoredCvContent          Generated CV text/markdown
     * @param tailoredCoverLetterContent Generated Cover Letter text
     * @param status                     Initial status (e.g. "DRAFT")
     */
    public ApplicationKit(AppUser user, JobPosting jobPosting, Integer matchScore,
                          String matchReasons, String tailoredCvContent,
                          String tailoredCoverLetterContent, String status) {
        this.user = user;
        this.jobPosting = jobPosting;
        this.matchScore = matchScore;
        this.matchReasons = matchReasons;
        this.tailoredCvContent = tailoredCvContent;
        this.tailoredCoverLetterContent = tailoredCoverLetterContent;
        this.status = (status != null) ? status : "DRAFT";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically sets and refreshes the timestamp on insert and update.
     */
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public JobPosting getJobPosting() {
        return jobPosting;
    }

    public void setJobPosting(JobPosting jobPosting) {
        this.jobPosting = jobPosting;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(String matchReasons) {
        this.matchReasons = matchReasons;
    }

    public String getTailoredCvContent() {
        return tailoredCvContent;
    }

    public void setTailoredCvContent(String tailoredCvContent) {
        this.tailoredCvContent = tailoredCvContent;
    }

    public String getTailoredCoverLetterContent() {
        return tailoredCoverLetterContent;
    }

    public void setTailoredCoverLetterContent(String tailoredCoverLetterContent) {
        this.tailoredCoverLetterContent = tailoredCoverLetterContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

