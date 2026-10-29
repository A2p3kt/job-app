package dev.toluwalase.jobapp.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table (name = "app_users")
public class AppUser {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;

    @Email(message = "Must be a valid email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt hashed password for local authentication (nullable if logging in solely via OAuth2).
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Authentication source, e.g., "LOCAL", "GITHUB", or "GOOGLE".
     */
    @Column(name = "oauth_provider", nullable = false)
    private String oauthProvider = "LOCAL";

    /**
     * Authorization role, e.g., "ROLE_USER" or "ROLE_ADMIN".
     */
    @Column(nullable = false)
    private String role = "ROLE_USER";

    /**
     * Inverse side of the 1-to-1 relationship with MasterProfile.
     * CascadeType.ALL ensures deleting a user also cleans up their profile.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private MasterProfile masterProfile;

    public AppUser() {
    }

    public AppUser(String username, String email, String passwordHash, String oauthProvider, String role) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.oauthProvider = oauthProvider;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public MasterProfile getMasterProfile() {
        return masterProfile;
    }

    public void setMasterProfile(MasterProfile masterProfile) {
        this.masterProfile = masterProfile;
    }
    
}
