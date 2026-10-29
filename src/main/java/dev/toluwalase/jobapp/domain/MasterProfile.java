package dev.toluwalase.jobapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity 
@Table (name = "master_profiles")
public class MasterProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning side of the 1-to-1 link: creates the 'user_id' foreign key column in 'master_profiles'.
     * @JsonIgnore prevents infinite recursion when returning entities in REST controllers.
     */
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @NotBlank(message = "Full name is required")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * Comma-separated list of skills (e.g., "Java, Spring Boot, PostgreSQL, Docker").
     */
    @Column(name = "skills_list", columnDefinition = "TEXT")
    private String skillsList;

    /**
     * Comma-separated list of preferred job locations (e.g., "Helsinki, Espoo, Remote").
     */
    @Column(name = "preferred_locations", columnDefinition = "TEXT")
    private String preferredLocations;

    /**
     * Comma-separated list of target role tags (e.g., "Backend, Cloud, Software Engineer").
     */
    @Column(name = "preferred_tags", columnDefinition = "TEXT")
    private String preferredTags;

    public MasterProfile() {
    }

    public MasterProfile(AppUser user, String fullName, String phone, String headline,
                         String summary, String skillsList, String preferredLocations, String preferredTags) {
        this.user = user;
        this.fullName = fullName;
        this.phone = phone;
        this.headline = headline;
        this.summary = summary;
        this.skillsList = skillsList;
        this.preferredLocations = preferredLocations;
        this.preferredTags = preferredTags;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSkillsList() { return skillsList; }
    public void setSkillsList(String skillsList) { this.skillsList = skillsList; }

    public String getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(String preferredLocations) { this.preferredLocations = preferredLocations; }

    public String getPreferredTags() { return preferredTags; }
    public void setPreferredTags(String preferredTags) { this.preferredTags = preferredTags; }
}