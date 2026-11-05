package dev.toluwalase.jobapp.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an indexed job opening across any supported industry category.
 * Populated automatically via scheduled multi-source API scraping or manual test seeding.
 */
@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier provided by the external API/source (e.g., JobTech ID, Remotive ID).
     * Prevents duplicate insertions across recurring scraping runs.
     */
    @NotNull(message = "External ID is required")
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    /**
     * Industry vertical specialization category (e.g. TECHNOLOGY, LAW_LEGAL, SERVICE_OPERATIONS).
     */
    @NotNull(message = "Job category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobCategory category = JobCategory.TECHNOLOGY;

    @NotBlank(message = "Job title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Company name is required")
    @Column(nullable = false)
    private String company;

    /**
     * Physical location or remote designation (e.g., "Helsinki, Finland", "Remote").
     */
    private String location;

    /**
     * Complete job description containing role responsibilities and requirements (HTML sanitized).
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Normalized comma-separated tags/keywords extracted during ingestion (e.g., "Java, Spring Boot, Docker").
     */
    @Column(name = "extracted_tags", columnDefinition = "TEXT")
    private String extractedTags;

    /**
     * Canonical link to the original external job listing.
     */
    @Column(name = "source_url")
    private String sourceUrl;

    /**
     * Name of the data source provider (e.g., "JobTech", "TheMuse", "Remotive", "ManualSeed").
     */
    @Column(name = "source_name")
    private String sourceName;

    /**
     * Timestamp indicating when this listing was fetched and persisted.
     */
    @Column(name = "scraped_at")
    private LocalDateTime scrapedAt;

    /**
     * Default no-argument constructor required by JPA.
     */
    public JobPosting() {
    }

    /**
     * Parameterized constructor for creating a new JobPosting with explicit category.
     *
     * @param externalId    Unique external identifier
     * @param category      Industry specialization category
     * @param title         Job title
     * @param company       Hiring company
     * @param location      Job location or remote status
     * @param description   Full role description
     * @param extractedTags Normalized technical keywords
     * @param sourceUrl     Link to external posting
     * @param sourceName    Provider identifier
     * @param scrapedAt     Ingestion timestamp
     */
    public JobPosting(String externalId, JobCategory category, String title, String company,
                      String location, String description, String extractedTags,
                      String sourceUrl, String sourceName, LocalDateTime scrapedAt) {
        this.externalId = externalId;
        this.category = (category != null) ? category : JobCategory.TECHNOLOGY;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.extractedTags = extractedTags;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
        this.scrapedAt = scrapedAt;
    }

    /**
     * Overloaded constructor defaulting category to TECHNOLOGY.
     */
    public JobPosting(String externalId, String title, String company, String location,
                      String description, String extractedTags, String sourceUrl,
                      String sourceName, LocalDateTime scrapedAt) {
        this(externalId, JobCategory.TECHNOLOGY, title, company, location, description, extractedTags, sourceUrl, sourceName, scrapedAt);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public JobCategory getCategory() {
        return category;
    }

    public void setCategory(JobCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtractedTags() {
        return extractedTags;
    }

    public void setExtractedTags(String extractedTags) {
        this.extractedTags = extractedTags;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(LocalDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }
}
