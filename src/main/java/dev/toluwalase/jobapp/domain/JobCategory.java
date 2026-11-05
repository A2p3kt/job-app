package dev.toluwalase.jobapp.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumeration representing the career specialization categories supported by JobApp.
 * Encapsulates human-readable display names, API query strings for external job portals,
 * and curated industry-specific keyword dictionaries for tag extraction and matching.
 */
public enum JobCategory {

    TECHNOLOGY(
            "Technology & IT",
            "developer OR software OR backend OR frontend OR devops OR cloud",
            "Software Engineering",
            "software-dev",
            Arrays.asList("Java", "Spring Boot", "Python", "Django", "PostgreSQL", "Docker", "AWS", "Kubernetes", "Linux", "React", "TypeScript", "REST", "Git", "CI/CD", "Cloud", "Microservices", "SQL")
    ),

    MARKETING(
            "Marketing & Communications",
            "marketing OR communications OR \"content creator\" OR copywriter OR seo",
            "Marketing",
            "marketing",
            Arrays.asList("SEO", "Content Strategy", "Social Media", "Copywriting", "Google Analytics", "Brand Strategy", "Campaigns", "Email Marketing", "CRM", "Market Research", "Public Relations", "B2B Marketing")
    ),

    LAW_LEGAL(
            "Law & Legal Services",
            "jurist OR legal OR compliance OR \"paralegal\" OR lawyer",
            "Legal",
            "all-others",
            Arrays.asList("Contract Law", "Compliance", "GDPR", "Corporate Law", "Litigation", "Due Diligence", "Legal Research", "Negotiation", "Regulatory Affairs", "Intellectual Property", "Policy Drafting")
    ),

    SERVICE_OPERATIONS(
            "Service & Facility Operations",
            "cleaning OR cleaner OR facility OR maintenance OR custodian OR housekeeping",
            "Facilities",
            "all-others",
            Arrays.asList("Commercial Cleaning", "Sanitization", "Equipment Maintenance", "Safety Standards", "Waste Management", "Floor Care", "Inventory Control", "Customer Service", "Shift Work", "Occupational Safety")
    ),

    BUSINESS_FINANCE(
            "Business & Finance",
            "finance OR accountant OR \"financial analyst\" OR banking OR auditor",
            "Accounting and Finance",
            "finance-legal",
            Arrays.asList("Financial Analysis", "Accounting", "Budgeting", "IFRS", "Auditing", "Forecasting", "Excel", "Payroll", "Financial Modeling", "Reporting", "Risk Assessment", "ERP")
    ),

    HEALTHCARE(
            "Healthcare & Nursing",
            "nurse OR nursing OR healthcare OR caregiver OR clinical",
            "Healthcare",
            "all-others",
            Arrays.asList("Patient Care", "Clinical Protocols", "Medication Administration", "First Aid", "Medical Documentation", "Patient Assessment", "Triage", "Infection Control", "Care Planning", "Vital Signs")
    );

    private final String displayName;
    private final String searchQuery;
    private final String theMuseCategory;
    private final String remotiveCategory;
    private final List<String> keywords;

    /**
     * Parameterized constructor for JobCategory enum entries.
     *
     * @param displayName      User-friendly UI display title
     * @param searchQuery      Search query string for JobTech Nordics API
     * @param theMuseCategory  Category filter parameter for The Muse API
     * @param remotiveCategory Category slug parameter for Remotive API
     * @param keywords         Curated industry-specific keyword dictionary
     */
    JobCategory(String displayName, String searchQuery, String theMuseCategory,
                String remotiveCategory, List<String> keywords) {
        this.displayName = displayName;
        this.searchQuery = searchQuery;
        this.theMuseCategory = theMuseCategory;
        this.remotiveCategory = remotiveCategory;
        this.keywords = Collections.unmodifiableList(keywords);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getTheMuseCategory() {
        return theMuseCategory;
    }

    public String getRemotiveCategory() {
        return remotiveCategory;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}

