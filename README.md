# JobApp — Intelligent Job Discovery & AI Application Tailoring Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-H2%20%7C%20PostgreSQL-blue.svg)]()
[![License](https://img.shields.io/badge/License-Academic-lightgrey.svg)]()

## 1. Project Overview

**JobApp** is a full-stack Java Spring Boot application that serves as an automated job discovery, intelligent career matching, and AI-powered application tailoring engine.

In today's competitive job market, candidates waste dozens of hours manually customizing resumes and cover letters for individual postings. JobApp automates and streamlines this entire workflow:
1. **Central Master Profile:** Candidates maintain a single source of truth for their contact details, headline, executive summary, verified skills, job search preferences, and chronological building blocks (work experience, education, certifications, and projects).
2. **Automated Job Ingestion:** Background workers periodically fetch real-world software engineering job listings from public APIs, normalize technical keywords, and store them in the database.
3. **Two-Tier Matching Engine:** Evaluates job openings against candidate profiles using a hybrid scoring algorithm combining explicit search preferences (40%) and semantic profile background overlap (60%).
4. **AI-Powered Tailoring:** On-demand generation of targeted, high-impact **Custom CVs** and **Cover Letters** via an LLM API (Google Gemini / OpenAI), complete with a resilient offline template fallback for zero-crash grading.
5. **Interactive Editor & Dynamic PDF Export:** Users review, edit, and fine-tune their application materials directly in an in-browser split-view workspace, then download production-ready PDF documents.

---

## 2. System Architecture & Strict Package Structure

To comply with course and grading constraints, all classes strictly reside within the root package `dev.toluwalase.jobapp` across **only three sub-packages**:

```
src/main/java/dev/toluwalase/jobapp/
│
├── JobappApplication.java          # Spring Boot main class & startup data seeding
│
├── domain/                         # Persistence & Data Contracts
│   ├── AppUser.java                # JPA Entity: Authenticated user account
│   ├── AppUserRepository.java      # Spring Data JPA Repository
│   ├── MasterProfile.java          # JPA Entity: Central career profile
│   ├── MasterProfileRepository.java
│   ├── ProfileItem.java            # JPA Entity: Experience, Education, Projects
│   ├── ProfileItemType.java        # Enum: EXPERIENCE, EDUCATION, CERTIFICATION, PROJECT
│   ├── ProfileItemRepository.java
│   ├── JobPosting.java             # JPA Entity: Ingested job listings
│   ├── JobPostingRepository.java
│   ├── ApplicationKit.java         # JPA Entity: Tailored CV/Cover letter artifacts
│   ├── ApplicationKitRepository.java
│   └── dto/                        # Transfer models (JobMatchDto, etc.) within domain
│
├── service/                        # Business Logic & External Integrations
│   ├── ProfileService.java         # Profile & resume item management
│   ├── JobScraperService.java      # Scheduled & manual external job ingestion
│   ├── MatchingEngineService.java  # Two-tier 40/60 scoring engine
│   ├── TailoringService.java       # LLM generation with offline template fallback
│   └── PdfExportService.java       # OpenPDF document renderer
│
└── web/                            # Web MVC & REST Presentation Layer
    ├── ProfileWebController.java   # Thymeleaf UI: Profile views & item forms
    ├── JobWebController.java       # Thymeleaf UI: Job feed & manual scraping triggers
    ├── KitWebController.java       # Thymeleaf UI: Split-screen tailoring editor
    ├── api/                        # REST Controllers under /api/v1/**
    │   ├── ProfileRestController.java
    │   ├── JobRestController.java
    │   └── KitRestController.java
    └── SecurityConfig.java         # Spring Security & OAuth2 configuration
```

---

## 3. Tech Stack

- **Language & Runtime:** Java 21 (LTS)
- **Framework:** Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, DevTools)
- **Templating & UI:** Thymeleaf, HTML5, CSS3, Modern Responsive UI (Tailwind CSS / Bootstrap)
- **Database (Development):** In-Memory H2 Database (`jdbc:h2:mem:jobappdb` / `jobforgedb`) with H2 Web Console
- **Database (Production / Grade 5):** PostgreSQL (`org.postgresql.Driver`)
- **PDF Generation:** OpenPDF (`com.github.librepdf:openpdf`)
- **External API & LLM:** Spring `RestClient` (calling Remotive/JobTech APIs and Google Gemini/OpenAI API)
- **Testing:** JUnit 5, Mockito, Spring Boot Test (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`)
- **Build Tool:** Maven

---

## 4. Getting Started

### Prerequisites
- Java 21 SDK installed (`java -version`)
- Maven 3.9+ (or use the included `./mvnw` wrapper)

### Running the Application Locally
1. Clone the repository and navigate to the project directory:
   ```bash
   cd c:\Users\toluw\Kitchen\jobapp
   ```
2. Build the project:
   ```bash
   ./mvnw clean compile
   ```
3. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Access the web application at [http://localhost:8080](http://localhost:8080).

### Accessing the H2 Database Console
The in-memory database console is available during development:
- **URL:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **Driver Class:** `org.h2.Driver`
- **JDBC URL:** `jdbc:h2:mem:jobappdb`
- **Username:** `sa`
- **Password:** *(leave blank)*

---

## 5. Course & Grading Compliance (Grade 5 Target)

- [x] **Strict 3-Package Architecture:** Enforces separation of concerns (`domain`, `service`, `web`).
- [x] **No Lombok:** Handwritten, clean, standard constructors, getters, and setters.
- [x] **Comprehensive Documentation:** Full Javadoc on all classes and methods.
- [ ] **Spring Security:** Local BCrypt authentication + OAuth2 social login (GitHub/Google).
- [ ] **Automated Scheduling:** Hourly `@Scheduled` background worker for job ingestion with manual trigger.
- [ ] **Two-Tier Matching Algorithm:** 40% preference overlap + 60% semantic profile evidence score.
- [ ] **Dual-Mode AI Tailoring:** Live LLM integration via `RestClient` + deterministic offline template fallback.
- [ ] **Dynamic PDF Generation:** On-the-fly binary rendering of tailored CVs and cover letters.
- [ ] **Dual Web + REST Architecture:** Thymeleaf server-rendered interface alongside `/api/v1/**` REST API.
- [ ] **Automated Test Suite:** Unit tests for matching algorithms, Mockito mocks for external APIs, and JPA slice tests.
