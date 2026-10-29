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

@SpringBootApplication
public class JobappApplication {

	private static final Logger log = LoggerFactory.getLogger(JobappApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(JobappApplication.class, args);
	}

	/**
     * Seeds a test AppUser and linked MasterProfile into H2 on startup.
     */
    @Bean
    public CommandLineRunner demoData(AppUserRepository userRepository,
                                      MasterProfileRepository profileRepository) {
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

                // 2. Create and save the linked MasterProfile (references demoUser -> user_id)
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
                profileRepository.save(demoProfile);

                log.info("Seeded AppUser (ID: {}) and linked MasterProfile (ID: {}) into H2!",
                        demoUser.getId(), demoProfile.getId());
            }
        };
	}
}
