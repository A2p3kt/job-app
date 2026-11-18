package dev.toluwalase.jobapp;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.ApplicationKit;
import dev.toluwalase.jobapp.domain.ApplicationKitRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.domain.JobPosting;
import dev.toluwalase.jobapp.domain.JobPostingRepository;
import dev.toluwalase.jobapp.domain.MasterProfile;
import dev.toluwalase.jobapp.domain.MasterProfileRepository;
import dev.toluwalase.jobapp.domain.ProfileItem;
import dev.toluwalase.jobapp.domain.ProfileItemRepository;
import dev.toluwalase.jobapp.domain.ProfileItemType;
import dev.toluwalase.jobapp.web.SecurityConfig;

/**
 * Main entry point for the JobApp application.
 * Contains startup seeding for development in H2 across all supported industry
 * categories.
 */
@SpringBootApplication
@EnableScheduling
public class JobappApplication {

        private static final Logger log = LoggerFactory.getLogger(JobappApplication.class);

        public static void main(String[] args) {
                SpringApplication.run(JobappApplication.class, args);
        }

        /**
         * Configures a default RestClient.Builder bean for web requests and API
         * scrapers.
         *
         * @return RestClient.Builder instance
         */
        @Bean
        @Scope("prototype")
        public RestClient.Builder restClientBuilder() {
                return RestClient.builder()
                                .defaultHeader(HttpHeaders.USER_AGENT,
                                                "JobApp/1.0 (Job Market Ingestion Engine; +https://github.com/A2p3kt/job-app)");
        }

        /**
         * Configures an ObjectMapper bean for JSON parsing.
         *
         * @return ObjectMapper instance
         */
        @Bean
        public ObjectMapper objectMapper() {
                return new ObjectMapper();
        }

        /**
         * Seeds initial development data (AppUser, MasterProfile, ProfileItems,
         * multi-industry JobPostings, and sample ApplicationKits) into H2 on startup.
         *
         * @param userRepository    Repository for AppUser persistence
         * @param profileRepository Repository for MasterProfile persistence
         * @param itemRepository    Repository for ProfileItem persistence
         * @param jobRepository     Repository for JobPosting persistence
         * @param kitRepository     Repository for ApplicationKit persistence
         * @return CommandLineRunner callback
         */
        @Bean
        public CommandLineRunner demoData(AppUserRepository userRepository,
                        MasterProfileRepository profileRepository,
                        ProfileItemRepository itemRepository,
                        JobPostingRepository jobRepository,
                        ApplicationKitRepository kitRepository) {
                return (args) -> {
                        // Guard: verify database is completely empty before adding any demo data
                        long userCount = userRepository.count();
                        long profileCount = profileRepository.count();
                        long itemCount = itemRepository.count();
                        long jobCount = jobRepository.count();
                        long kitCount = kitRepository.count();

                        if (userCount > 0 || profileCount > 0 || itemCount > 0 || jobCount > 0 || kitCount > 0) {
                                log.info("Database is not empty (Users: {}, Profiles: {}, Items: {}, Jobs: {}, Kits: {}). Skipping initial demo data seeding.",
                                                userCount, profileCount, itemCount, jobCount, kitCount);
                                return;
                        }

                        log.info("Database is empty. Initializing initial demo dataset...");

                        // 1. Seed Demo User & Career Profile
                        AppUser demoUser = new AppUser(
                                        "tolu",
                                        "hello@toluwalase.dev",
                                        SecurityConfig.hashPassword("password123"),
                                        "LOCAL",
                                        "ROLE_USER");
                        userRepository.save(demoUser);

                        MasterProfile demoProfile = new MasterProfile(
                                        demoUser,
                                        JobCategory.TECHNOLOGY,
                                        "Toluwalase Ayoola",
                                        "+358 40 123 4567",
                                        "Lead Cloud Architect & Senior Distributed Systems Engineer",
                                        "Pragmatic, results-driven software engineer with 6+ years of experience architecting high-throughput distributed microservices, event-driven streaming architectures, and resilient cloud infrastructure using Java 21, Spring Boot 4, Kubernetes, and AWS. Proven track record of scaling low-latency APIs processing 25,000+ req/s, cutting cloud infrastructure costs by 35%, and integrating generative AI to automate mission-critical workflows.",
                                        "Java 21, Spring Boot 4, Spring Cloud, Kubernetes, Docker, AWS (ECS, EKS, Lambda, RDS), PostgreSQL, Apache Kafka, Redis, Terraform, REST & GraphQL APIs, Microservices, CI/CD, Git, Linux",
                                        "Helsinki, Espoo, Tampere, Stockholm, Remote",
                                        "Backend, Cloud, Distributed Systems, Software Engineer, Senior, Lead, Architect");

                                // Quality Experience 1
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.EXPERIENCE,
                                                "Lead Backend Engineer & Distributed Systems Architect",
                                                "Nordic Cloud Platforms Oy",
                                                "Helsinki, Finland",
                                                "2022-03",
                                                "Present",
                                                "Architected high-throughput, fault-tolerant payment and transaction microservices in Java 21 and Spring Boot, serving 25,000+ requests/sec with a 99.99% uptime SLA. Led the containerized migration of legacy monolithic workloads to Amazon EKS using Docker and Terraform, reducing cloud operational overhead by 35%. Mentored 8 junior and mid-level engineers, established automated CI/CD quality gates, and drove zero-downtime deployment pipelines."));

                                // Quality Experience 2
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.EXPERIENCE,
                                                "Senior Java Software Engineer",
                                                "FinTech Solutions Nordics",
                                                "Espoo, Finland",
                                                "2019-08",
                                                "2022-02",
                                                "Engineered resilient event-driven data streaming pipelines utilizing Apache Kafka, Spring Data JPA, and PostgreSQL for real-time fraud monitoring. Optimized complex SQL queries and distributed Redis caching layers, cutting P99 database query response latency from 450ms down to 28ms. Implemented OpenAPI 3.0 contracts and OAuth2/JWT security filters across 12 microservices."));

                                // Top Education 1
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.EDUCATION,
                                                "M.Sc. in Computer Science (Distributed Systems & Cloud Computing)",
                                                "Aalto University — School of Science",
                                                "Espoo, Finland",
                                                "2017-09",
                                                "2019-06",
                                                "Graduated with Distinction (Honors). Master's Thesis: 'Resilience and Latency Trade-offs in Event-Driven Distributed Microservice Architectures'. Coursework: Advanced Distributed Systems, Fault-Tolerant Computing, Cloud Architecture, High-Performance Parallel Computing."));

                                // Top Education 2
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.EDUCATION,
                                                "B.Sc. in Software Engineering",
                                                "University of Helsinki",
                                                "Helsinki, Finland",
                                                "2014-09",
                                                "2017-06",
                                                "Graduated Magna Cum Laude. Core studies in Object-Oriented Software Design, Operating Systems, Algorithm Complexity, Database Management Systems, and Computer Networks."));

                                // Nice Certifications
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.CERTIFICATION,
                                                "AWS Certified Solutions Architect — Professional",
                                                "Amazon Web Services (AWS)",
                                                "Online / Verified",
                                                "2023-04",
                                                "2026-04",
                                                "Validation of advanced technical skills and experience in designing distributed applications and systems on the AWS cloud platform, multi-tier architectures, and high availability systems."));

                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.CERTIFICATION,
                                                "Certified Kubernetes Administrator (CKA)",
                                                "Cloud Native Computing Foundation & Linux Foundation",
                                                "Online / Verified",
                                                "2023-11",
                                                "2026-11",
                                                "Demonstrated hands-on competence in Kubernetes cluster architecture, workload deployment, networking, storage, security, troubleshooting, and maintenance."));

                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.CERTIFICATION,
                                                "Oracle Certified Professional: Java SE 21 Developer",
                                                "Oracle",
                                                "Online / Verified",
                                                "2024-01",
                                                "Lifetime",
                                                "Comprehensive validation of modern Java 21 language features including Virtual Threads (Project Loom), Pattern Matching, Record Patterns, Concurrency, and JVM memory internals."));

                                // Projects
                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.PROJECT,
                                                "JobApp — Enterprise AI Job Matcher & Application Engine",
                                                "Open Source / Production Showcase",
                                                "Helsinki, Finland",
                                                "2024-08",
                                                "Present",
                                                "Architected and built an end-to-end full-stack career acceleration platform with Spring Boot 4, Tailwind CSS, OpenPDF, and Google Gemini AI. Features automated multi-source job ingestion, two-tier deterministic 0–100 candidate ranking, and instant PDF tailoring."));

                                demoProfile.addItem(new ProfileItem(
                                                demoProfile,
                                                ProfileItemType.PROJECT,
                                                "Distributed Event Broker & In-Memory Key-Value Cache",
                                                "Independent Research",
                                                "Remote",
                                                "2023-01",
                                                "2023-07",
                                                "Developed a lightweight, Raft-consensus replicated in-memory storage engine in Java 21 using virtual threads and non-blocking I/O (NIO), reaching 120,000 ops/sec under network partitioned conditions."));

                                profileRepository.save(demoProfile);

                                log.info("Seeded AppUser (ID: {}), MasterProfile (ID: {}, Specialization: {}), and {} ProfileItems into H2!",
                                                demoUser.getId(), demoProfile.getId(), demoProfile.getSpecialization(),
                                                itemRepository.count());

                        // 2. Seed Multi-Industry Job Postings
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
                                                                LocalDateTime.now().minusHours(2)),
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
                                                                LocalDateTime.now().minusHours(5)),
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
                                                                LocalDateTime.now().minusHours(6)),
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
                                                                LocalDateTime.now().minusHours(10)),
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
                                                                LocalDateTime.now().minusHours(14)),
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
                                                                LocalDateTime.now().minusHours(18)),
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
                                                                LocalDateTime.now().minusHours(22)));

                                jobRepository.saveAll(sampleJobs);
                                log.info("Seeded {} multi-industry JobPostings into H2!", jobRepository.count());

                        // 3. Seed Sample Application Kits for Demo User
                        JobPosting job1 = jobRepository.findByExternalId("seed-job-001").orElse(null);
                        JobPosting job2 = jobRepository.findByExternalId("seed-job-002").orElse(null);

                        if (job1 != null) {
                                String cv1 = "# Toluwalase Ayoola\n"
                                                + "hello@toluwalase.dev | +358 40 123 4567 | Helsinki, Finland | linkedin.com/in/toluwalase\n\n"
                                                + "## Professional Summary\n"
                                                + "Lead Cloud Architect & Senior Distributed Systems Engineer with 6+ years of experience architecting resilient Java 21 and Spring Boot platforms. Deep expertise in PostgreSQL transactional databases, microservices, and Docker containerization.\n\n"
                                                + "## Core Competencies\n"
                                                + "- Languages & Frameworks: Java 21, Spring Boot 4, Spring Data JPA, RESTful APIs, Microservices\n"
                                                + "- Databases & Messaging: PostgreSQL, Redis, Apache Kafka\n"
                                                + "- Cloud & DevOps: Docker, Kubernetes, AWS, Terraform, CI/CD, Linux\n\n"
                                                + "## Professional Experience\n"
                                                + "### Lead Backend Engineer & Distributed Systems Architect\n"
                                                + "**Nordic Cloud Platforms Oy** | Helsinki, Finland | 2022-03 – Present\n"
                                                + "- Architected microservices in Java 21 and Spring Boot handling 25,000+ req/s with 99.99% uptime.\n"
                                                + "- Containerized mission-critical services using Docker and orchestrated deployments on Kubernetes.\n"
                                                + "- Designed PostgreSQL schema migrations and optimized query execution with Spring Data JPA.\n\n"
                                                + "### Senior Java Software Engineer\n"
                                                + "**FinTech Solutions Nordics** | Espoo, Finland | 2019-08 – 2022-02\n"
                                                + "- Built event-driven data streaming pipelines with Apache Kafka, Spring Boot, and PostgreSQL.\n"
                                                + "- Slashed P99 database query response latency from 450ms down to 28ms using Redis caching.\n\n"
                                                + "## Education\n"
                                                + "- **M.Sc. in Computer Science (Distributed Systems)**, Aalto University (2017 – 2019, Honors)\n"
                                                + "- **B.Sc. in Software Engineering**, University of Helsinki (2014 – 2017, Magna Cum Laude)\n\n"
                                                + "## Certifications\n"
                                                + "- AWS Certified Solutions Architect — Professional (2023 – 2026)\n"
                                                + "- Certified Kubernetes Administrator (CKA) (2023 – 2026)\n"
                                                + "- Oracle Certified Professional: Java SE 21 Developer";

                                String cl1 = "Dear Hiring Team at Nordic Financial Technologies,\n\n"
                                                + "I am writing to express my enthusiastic interest in the Backend Developer position at Nordic Financial Technologies. With over six years of hands-on experience designing and scaling resilient backend platforms with Java 21 and Spring Boot, I am eager to contribute immediately to your core platform engineering efforts.\n\n"
                                                + "In my recent role as Lead Backend Engineer at Nordic Cloud Platforms Oy, I architected distributed microservices processing more than 25,000 requests per second with high availability. I specialize in designing transactional data models in PostgreSQL, optimizing ORM layers with Spring Data JPA, and packaging services into lightweight Docker containers for seamless deployment. My background directly aligns with your technical stack and engineering standards.\n\n"
                                                + "I would welcome the opportunity to discuss how my technical leadership and microservices experience can help Nordic Financial Technologies scale its financial infrastructure. Thank you for your time and consideration.\n\n"
                                                + "Warm regards,\n\n"
                                                + "Toluwalase Ayoola\n"
                                                + "hello@toluwalase.dev | +358 40 123 4567";

                                ApplicationKit kit1 = new ApplicationKit(
                                                demoUser,
                                                job1,
                                                96,
                                                "Exceptional alignment with Java 21 and Spring Boot stack. Deep experience in PostgreSQL transactional databases, Docker containerization, RESTful microservices, and high-availability cloud platforms.",
                                                cv1,
                                                cl1,
                                                "FINALIZED");
                                kitRepository.save(kit1);
                        }

                        if (job2 != null) {
                                String cv2 = "# Toluwalase Ayoola\n"
                                                + "hello@toluwalase.dev | +358 40 123 4567 | Helsinki, Finland | linkedin.com/in/toluwalase\n\n"
                                                + "## Professional Summary\n"
                                                + "Senior Cloud & DevOps Engineer with 6+ years of experience designing and managing scalable cloud infrastructure on AWS. Certified Kubernetes Administrator (CKA) and AWS Certified Solutions Architect Professional.\n\n"
                                                + "## Core Competencies\n"
                                                + "- Cloud Architecture: AWS (ECS, EKS, Lambda, RDS, S3, IAM, CloudWatch)\n"
                                                + "- Infrastructure as Code: Terraform, Docker, Kubernetes, Linux administration\n"
                                                + "- CI/CD & Automation: GitHub Actions, Python scripting, Bash, monitoring\n\n"
                                                + "## Experience Highlights\n"
                                                + "- Led containerized cloud migration to Amazon EKS using Terraform, cutting infrastructure cost by 35%.\n"
                                                + "- Designed zero-downtime CI/CD deployment pipelines and automated infrastructure provisioning.\n"
                                                + "- Implemented infrastructure monitoring and automated alerting using Prometheus and Grafana.";

                                String cl2 = "Dear Aura Cloud Solutions Hiring Team,\n\n"
                                                + "I am thrilled to apply for the Cloud Infrastructure & DevOps Engineer position at Aura Cloud Solutions. Having engineered automated AWS architectures and managed containerized workloads across my career, I am confident in my ability to elevate your cloud infrastructure.\n\n"
                                                + "Holding both the AWS Certified Solutions Architect — Professional and Certified Kubernetes Administrator (CKA) certifications, I bring deep practical experience in Terraform automation, Docker packaging, and resilient Linux system administration. At Nordic Cloud Platforms Oy, I led our cloud migration to Kubernetes on AWS, drastically increasing developer velocity while improving system fault tolerance.\n\n"
                                                + "I look forward to discussing how my cloud infrastructure and automation expertise can support Aura Cloud's mission.\n\n"
                                                + "Sincerely,\n\n"
                                                + "Toluwalase Ayoola\n"
                                                + "hello@toluwalase.dev | +358 40 123 4567";

                                ApplicationKit kit2 = new ApplicationKit(
                                                demoUser,
                                                job2,
                                                92,
                                                "Outstanding match for AWS infrastructure, Docker containerization, CI/CD automation, and Linux administration. Validated by AWS Certified Solutions Architect and CKA credentials.",
                                                cv2,
                                                cl2,
                                                "DRAFT");
                                kitRepository.save(kit2);
                        }

                        log.info("Seeded {} sample ApplicationKits into H2!", kitRepository.count());
                };
        }
}
