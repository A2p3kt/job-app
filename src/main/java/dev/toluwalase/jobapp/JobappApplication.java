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
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;

/**
 * Main entry point for the JobApp application.
 * Contains startup seeding for development in H2.
 */
@SpringBootApplication
public class JobappApplication {

    private static final Logger log = LoggerFactory.getLogger(JobappApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JobappApplication.class, args);
    }

    /**
     * Seeds initial development data (AppUser, MasterProfile, ProfileItems, and JobPostings) into H2 on startup.
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

                log.info("Seeded AppUser (ID: {}), MasterProfile (ID: {}), and {} ProfileItems into H2!",
                        demoUser.getId(), demoProfile.getId(), itemRepository.count());
            }

            // 2. Seed Sample Job Postings
            if (jobRepository.count() == 0) {
                List<JobPosting> sampleJobs = List.of(
                        new JobPosting(
                                "seed-job-001",
                                "Junior Backend Developer",
                                "Nordic Financial Technologies",
                                "Helsinki, Finland (Hybrid)",
                                "We are seeking an enthusiastic Junior Backend Developer to join our core platform engineering team in Helsinki. You will build and scale resilient RESTful APIs in Java 21 and Spring Boot, interact with PostgreSQL transactional databases using Spring Data JPA, and package services into Docker containers. Mentorship provided by senior architects.",
                                "Java, Spring Boot, PostgreSQL, Docker, REST, Git, Microservices",
                                "https://nordicfintech.example.com/careers/backend-dev",
                                "SeedData",
                                LocalDateTime.now().minusHours(2)
                        ),
                        new JobPosting(
                                "seed-job-002",
                                "Cloud Infrastructure & DevOps Engineer",
                                "Aura Cloud Solutions",
                                "Espoo, Finland",
                                "Aura Cloud is looking for a Cloud Infrastructure Engineer to design, deploy, and maintain automated AWS architectures. Requirements include strong Linux foundations, hands-on containerization experience with Docker, scripting proficiency in Python or Bash, and knowledge of modern CI/CD pipelines. AWS certifications (Practitioner or Solutions Architect) are considered a significant advantage.",
                                "AWS, Docker, Linux, Python, Terraform, CI/CD, Cloud, Git",
                                "https://auracloud.example.com/jobs/cloud-engineer",
                                "SeedData",
                                LocalDateTime.now().minusHours(5)
                        ),
                        new JobPosting(
                                "seed-job-003",
                                "Full-Stack Software Engineer (Java & React)",
                                "Kide Health Innovations",
                                "Remote (Finland / EU)",
                                "Join our mission-driven digital health startup as a Full-Stack Engineer. In this role, you will implement client-facing interfaces with React and TypeScript while extending reliable enterprise backend services built on Spring Boot and PostgreSQL. Experience with Docker and microservice patterns is required.",
                                "Java, Spring Boot, React, TypeScript, Docker, PostgreSQL, REST",
                                "https://kidehealth.example.com/careers/fullstack",
                                "SeedData",
                                LocalDateTime.now().minusHours(8)
                        ),
                        new JobPosting(
                                "seed-job-004",
                                "Python Data Platform Developer",
                                "Baltic Insights Oy",
                                "Helsinki, Finland",
                                "Baltic Insights is hiring a Python Developer to join our analytics pipeline team. You will craft scalable ingestion workflows using Python, Django REST Framework, and AWS Lambda serverless functions, connected to PostgreSQL databases. Ideal candidates possess a strong grasp of relational database design, clean coding habits, and Docker deployment workflows.",
                                "Python, Django, PostgreSQL, Docker, AWS Lambda, REST, SQL",
                                "https://balticinsights.example.com/careers/python-developer",
                                "SeedData",
                                LocalDateTime.now().minusHours(12)
                        ),
                        new JobPosting(
                                "seed-job-005",
                                "Embedded Systems Firmware Engineer",
                                "Polar Robotics Oy",
                                "Tampere, Finland",
                                "Polar Robotics designs autonomous industrial navigation units. We are looking for an Embedded Systems Engineer proficient in modern C++ and C to develop real-time firmware for microcontroller hardware and low-power telemetry sensors. Experience with FreeRTOS and oscilloscope debugging required.",
                                "C++, Embedded, RTOS, Hardware, Robotics, C",
                                "https://polarrobotics.example.com/careers/embedded-engineer",
                                "SeedData",
                                LocalDateTime.now().minusHours(24)
                        )
                );

                jobRepository.saveAll(sampleJobs);
                log.info("Seeded {} sample JobPostings into H2!", jobRepository.count());
            }
        };
    }
}
