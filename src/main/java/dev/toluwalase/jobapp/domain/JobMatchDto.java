package dev.toluwalase.jobapp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object encapsulating a JobPosting paired with its algorithmic match calculation.
 * Includes composite 0–100 match score, two-tier score breakdowns (Preference vs. Evidence),
 * and transparent keyword match rationale strings.
 * Implements Comparable to sort descending by match score.
 */
public class JobMatchDto implements Comparable<JobMatchDto> {

    private JobPosting job;
    private int matchScore;
    private int preferenceScore;
    private int evidenceScore;
    private List<String> matchReasons;

    /**
     * Default no-argument constructor.
     */
    public JobMatchDto() {
        this.matchReasons = new ArrayList<>();
    }

    /**
     * Parameterized constructor initializing all scoring dimensions.
     *
     * @param job             Target job posting entity
     * @param matchScore      Composite normalized score (0–100)
     * @param preferenceScore Preference alignment subscore (0–40)
     * @param evidenceScore   Profile evidence alignment subscore (0–60)
     * @param matchReasons    List of human-readable rationale statements
     */
    public JobMatchDto(JobPosting job, int matchScore, int preferenceScore, int evidenceScore, List<String> matchReasons) {
        this.job = job;
        this.matchScore = Math.max(0, Math.min(100, matchScore));
        this.preferenceScore = preferenceScore;
        this.evidenceScore = evidenceScore;
        this.matchReasons = (matchReasons != null) ? new ArrayList<>(matchReasons) : new ArrayList<>();
    }

    /**
     * Convenience constructor calculating composite score from preference and evidence tiers.
     *
     * @param job             Target job posting entity
     * @param preferenceScore Preference alignment subscore (0–40)
     * @param evidenceScore   Profile evidence alignment subscore (0–60)
     * @param matchReasons    List of human-readable rationale statements
     */
    public JobMatchDto(JobPosting job, int preferenceScore, int evidenceScore, List<String> matchReasons) {
        this(job, Math.max(0, Math.min(100, preferenceScore + evidenceScore)), preferenceScore, evidenceScore, matchReasons);
    }

    /**
     * Natural order sorts descending by match score (highest score first).
     *
     * @param other Target JobMatchDto to compare with
     * @return Negative if this is greater, positive if other is greater, 0 if equal
     */
    @Override
    public int compareTo(JobMatchDto other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(other.matchScore, this.matchScore);
    }

    // Explicit Getters and Setters (Strictly No Lombok)

    public JobPosting getJob() {
        return job;
    }

    public void setJob(JobPosting job) {
        this.job = job;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = Math.max(0, Math.min(100, matchScore));
    }

    public int getPreferenceScore() {
        return preferenceScore;
    }

    public void setPreferenceScore(int preferenceScore) {
        this.preferenceScore = preferenceScore;
    }

    public int getEvidenceScore() {
        return evidenceScore;
    }

    public void setEvidenceScore(int evidenceScore) {
        this.evidenceScore = evidenceScore;
    }

    public List<String> getMatchReasons() {
        return Collections.unmodifiableList(matchReasons);
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = (matchReasons != null) ? new ArrayList<>(matchReasons) : new ArrayList<>();
    }

    public void addMatchReason(String reason) {
        if (reason != null && !reason.isBlank()) {
            this.matchReasons.add(reason);
        }
    }
}

