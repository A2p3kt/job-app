package dev.toluwalase.jobapp;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Main entry point for the JobApp application.
 * Contains startup seeding for development in H2 across all supported industry categories.
 */
@SpringBootApplication
public class JobappApplication {

    private static final Logger log = LoggerFactory.getLogger(JobappApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JobappApplication.class, args);
    }

    /**
     * Seeds initial development data (AppUser, MasterProfile, ProfileItems, and multi-industry JobPostings) into H2 on startup.
     *
     * @param userRepository    Repository for AppUser persistence
     * @param profileRepository Repository for MasterProfile persistence
     * @param itemRepository    Repository for ProfileItem persistence
     * @param jobRepository     Repository for JobPosting persistence
     * @return CommandLineRunner callback
     */
    @Bean
    public CommandLineRunner demoData(AppUserRepository userRepository,
                                      MasterProfileRepository profileRepository,
                                      ProfileItemRepository itemRepository,
                                      JobPostingRepository jobRepository) {
        return (args) -> {
            // 1. Seed Demo User & Career Profile
            if (userRepository.count() == 0) {
                AppUser demoUser = new AppUser(
                        "tolu",
                        "hello@toluwalase.dev",
                        "temporary_plain_or_hashed_pw",
                        "LOCAL",
                        "ROLE_USER"
                );
                userRepository.save(demoUser);

                MasterProfile demoProfile = new MasterProfile(
                        demoUser,
                        JobCategory.TECHNOLOGY,
                        "Toluwalase Ayoola",
                        "+358 40 123 4567",
                        "Backend & Cloud Engineering Student",
                        "Hands-on software engineering student experienced in Python, Java Spring Boot, Docker, and cloud infrastructure.",
                        "Java, Spring Boot, Python, Django, PostgreSQL, Docker, AWS Lambda, Linux, React, Git",
                        "Helsinki, Espoo, Vantaa, Remote",
                        "Backend, Software Engineer, Cloud, Intern, Junior"
                );

                demoProfile.addItem(new ProfileItem(
                        demoProfile,
                        ProfileItemType.EXPERIENCE,
                        "Junior Backend Developer (Intern)",
                        "Nordic Tech Solutions",
                        "Helsinki, Finland",
                        "2024-05",
                        "2024-08",
                        "Developed RESTful microservices in Java Spring Boot. Integrated PostgreSQL with Spring Data JPA and containerized backend components using Docker."
                ));

                demoProfile.addItem(new ProfileItem(
                        demoProfile,
                        ProfileItemType.EDUCATION,
                        "B.Sc. in Business Information Technology (Software Development)",
                        "Haaga-Helia University of Applied Sciences",
                        "Helsinki, Finland",
                        "2023-01",
                        "2026-12",
                        "Focus on Enterprise Software Engineering, Cloud Architecture, Data Structures, and Modern Web Development."
                ));

                demoProfile.addItem(new ProfileItem(
                        demoProfile,
                        ProfileItemType.CERTIFICATION,
                        "AWS Certified Cloud Practitioner",
                        "Amazon Web Services",
                        "Online",
                        "2024-03",
                        "2027-03",
                        "Validation of cloud fluency, foundational security architecture, core AWS compute, and storage services."
                ));

                demoProfile.addItem(new ProfileItem(
                        demoProfile,
                        ProfileItemType.PROJECT,
                        "JobApp — Intelligent Application Tailoring Engine",
                        "Personal Project",
                        "Remote",
                        "2024-09",
                        "Present",
                        "Engineered full-stack Spring Boot system featuring automated job ingestion, deterministic two-tier matching, and on-demand LLM resume tailoring."
                ));

                profileRepository.save(demoProfile);

                log.info("Seeded AppUser (ID: {}), MasterProfile (ID: {}, Specialization: {}), and {} ProfileItems into H2!",
                        demoUser.getId(), demoProfile.getId(), demoProfile.getSpecialization(), itemRepository.count());
            }

            // 2. Seed Multi-Industry Job Postings
            if (jobRepository.count() == 0) {
                List<JobPosting> sampleJobs = List.of(
                        // Technology
                        new JobPosting(
                                "seed-job-001",
                                JobCategory.TECHNOLOGY,
                                "Junior Backend Developer",
                                "Nordic Financial Technologies",
                                "Helsinki, Finland (Hybrid)",
                                "We are seeking an enthusiastic Junior Backend Developer to join our core platform engineering team in Helsinki. You will build and scale resilient RESTful APIs in Java 21 and Spring Boot, interact with PostgreSQL transactional databases using Spring Data JPA, and package services into Docker containers. Mentorship provided by senior architects.",
                                "Java, Spring Boot, PostgreSQL, Docker, REST, Git, Microservices",
                                "https://nordicfintech.example.com/careers/backend-dev",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(2)
                        ),
                        new JobPosting(
                                "seed-job-002",
                                JobCategory.TECHNOLOGY,
                                "Cloud Infrastructure & DevOps Engineer",
                                "Aura Cloud Solutions",
                                "Espoo, Finland",
                                "Aura Cloud is looking for a Cloud Infrastructure Engineer to design, deploy, and maintain automated AWS architectures. Requirements include strong Linux foundations, hands-on containerization experience with Docker, scripting proficiency in Python or Bash, and knowledge of modern CI/CD pipelines. AWS certifications (Practitioner or Solutions Architect) are considered a significant advantage.",
                                "AWS, Docker, Linux, Python, Terraform, CI/CD, Cloud, Git",
                                "https://auracloud.example.com/jobs/cloud-engineer",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(5)
                        ),
                        // Service & Facility Operations
                        new JobPosting(
                                "seed-job-003",
                                JobCategory.SERVICE_OPERATIONS,
                                "Commercial Facility Operations & Cleaning Specialist",
                                "Nordic Clean & Facilities Oy",
                                "Helsinki, Finland",
                                "Nordic Clean is hiring a dedicated Facility Operations Specialist. Responsibilities include commercial sanitization, maintaining floor-care machinery, monitoring facility safety standards, and coordinating waste management according to environmental regulations.",
                                "Commercial Cleaning, Sanitization, Equipment Maintenance, Safety Standards, Shift Work",
                                "https://nordicclean.example.com/jobs/facility-specialist",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(6)
                        ),
                        // Law & Legal
                        new JobPosting(
                                "seed-job-004",
                                JobCategory.LAW_LEGAL,
                                "Corporate Legal & Compliance Specialist",
                                "Kallio Legal Advisors",
                                "Helsinki / Remote, Finland",
                                "Kallio Legal is seeking a junior-to-mid Legal Specialist to review commercial client vendor agreements, ensure GDPR compliance frameworks, and assist in due diligence processes for corporate transactions.",
                                "Contract Law, Compliance, GDPR, Corporate Law, Due Diligence, Policy Drafting",
                                "https://kalliolegal.example.com/careers/legal-specialist",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(10)
                        ),
                        // Marketing
                        new JobPosting(
                                "seed-job-005",
                                JobCategory.MARKETING,
                                "Digital Marketing & Communications Coordinator",
                                "Verve Media Helsinki",
                                "Espoo, Finland (Hybrid)",
                                "Verve Media is looking for a creative Communications Coordinator to manage multi-channel social media campaigns, track inbound traffic using Google Analytics, draft press releases, and optimize SEO rankings for enterprise client portfolios.",
                                "SEO, Content Strategy, Social Media, Google Analytics, Copywriting, Campaigns, CRM",
                                "https://vervemedia.example.com/careers/marketing-coordinator",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(14)
                        ),
                        // Business & Finance
                        new JobPosting(
                                "seed-job-006",
                                JobCategory.BUSINESS_FINANCE,
                                "Junior Financial Analyst & Accountant",
                                "Aurora Capital Partners",
                                "Helsinki, Finland",
                                "Aurora Capital is hiring a detail-oriented Financial Analyst to assist with monthly reporting, balance sheet reconciliations under IFRS guidelines, variance budgeting, and Excel financial modeling for portfolio entities.",
                                "Financial Analysis, Accounting, Budgeting, Excel, IFRS, Auditing, Reporting",
                                "https://auroracapital.example.com/jobs/financial-analyst",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(18)
                        ),
                        // Healthcare
                        new JobPosting(
                                "seed-job-007",
                                JobCategory.HEALTHCARE,
                                "Registered Nurse — Acute Care & Clinical Support",
                                "Meilahti Health Clinic",
                                "Helsinki, Finland",
                                "Meilahti Clinic has an open position for a registered nurse. Key tasks include primary patient triage, administering prescribed therapies, documenting clinical histories, and coordinating multidisciplinary patient care pathways.",
                                "Patient Care, Clinical Protocols, Medication Administration, First Aid, Medical Documentation, Vital Signs",
                                "https://meilahtihealth.example.com/careers/staff-nurse",
                                "ManualSeed",
                                LocalDateTime.now().minusHours(22)
                        )
                );

                jobRepository.saveAll(sampleJobs);
                log.info("Seeded {} multi-industry JobPostings into H2!", jobRepository.count());
            }
        };
    }
}
