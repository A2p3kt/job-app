package dev.toluwalase.jobapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Polymorphic resume building block belonging to a MasterProfile.
 * Represents discrete entries such as past job roles, academic degrees,
 * professional certifications, or technical projects.
 */
@Entity
@Table(name = "profile_items")
public class ProfileItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent MasterProfile that owns this item.
     * FetchType.LAZY prevents loading the entire profile unless requested.
     * JsonIgnore prevents circular serialization loops.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private MasterProfile profile;

    /**
     * Category of this item (EXPERIENCE, EDUCATION, CERTIFICATION, PROJECT).
     */
    @NotNull(message = "Item type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileItemType type;

    /**
     * Role title, degree name, certification title, or project name.
     */
    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    /**
     * Organization, employer, educational institution, or issuing authority.
     */
    private String organization;

    /**
     * Location where the role/education was performed (e.g., "Helsinki, Finland", "Remote").
     */
    private String location;

    /**
     * Start date formatted as YYYY-MM or human-readable year (e.g., "2023-01").
     */
    @Column(name = "start_date")
    private String startDate;

    /**
     * End date formatted as YYYY-MM or "Present" for current positions.
     */
    @Column(name = "end_date")
    private String endDate;

    /**
     * In-depth narrative, bullet points, responsibilities, or technologies used.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Default no-argument constructor required by JPA.
     */
    public ProfileItem() {
    }

    /**
     * Parameterized constructor for creating a new profile item without an ID.
     *
     * @param profile      Parent MasterProfile
     * @param type         Category of item
     * @param title        Title, role, or degree
     * @param organization Institution or company
     * @param location     Location or Remote
     * @param startDate    Start date string
     * @param endDate      End date string
     * @param description  Detailed accomplishments and technologies
     */
    public ProfileItem(MasterProfile profile, ProfileItemType type, String title,
                       String organization, String location, String startDate,
                       String endDate, String description) {
        this.profile = profile;
        this.type = type;
        this.title = title;
        this.organization = organization;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MasterProfile getProfile() {
        return profile;
    }

    public void setProfile(MasterProfile profile) {
        this.profile = profile;
    }

    public ProfileItemType getType() {
        return type;
    }

    public void setType(ProfileItemType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

