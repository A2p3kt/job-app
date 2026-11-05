package dev.toluwalase.jobapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
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
     * Seeds initial development data (AppUser, MasterProfile, and ProfileItems) into H2 on startup.
     *
     * @param userRepository    Repository for AppUser persistence
     * @param profileRepository Repository for MasterProfile persistence
     * @param itemRepository    Repository for ProfileItem persistence
     * @return CommandLineRunner callback
     */
    @Bean
    public CommandLineRunner demoData(AppUserRepository userRepository,
                                      MasterProfileRepository profileRepository,
                                      ProfileItemRepository itemRepository) {
        return (args) -> {
            if (userRepository.count() == 0) {
                // 1. Create and save the user account
                AppUser demoUser = new AppUser(
                        "tolu",
                        "hello@toluwalase.dev",
                        "temporary_plain_or_hashed_pw",
                        "LOCAL",
                        "ROLE_USER"
                );
                userRepository.save(demoUser);

                // 2. Create the linked MasterProfile
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

                // 3. Seed realistic ProfileItems (Experience, Education, Certification, Project)
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
        };
    }
}
