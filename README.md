# JobApp — Multi-Industry Job Discovery & AI Application Tailoring Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-H2%20%7C%20PostgreSQL-blue.svg)]()
[![License](https://img.shields.io/badge/License-Academic-lightgrey.svg)]()

## 1. Project Overview

**JobApp** is a full-stack Java Spring Boot application that serves as an automated job discovery, intelligent career matching, and AI-powered application tailoring engine supporting candidates across **multiple fields of specialization**:
- 💻 **Technology & IT** (`TECHNOLOGY`)
- 📈 **Marketing & Communications** (`MARKETING`)
- ⚖️ **Law & Legal Services** (`LAW_LEGAL`)
- 🧹 **Service & Facility Operations** (`SERVICE_OPERATIONS` — Cleaning, Maintenance, Hospitality)
- 💼 **Business, Administration & Finance** (`BUSINESS_FINANCE`)
- 🏥 **Healthcare & Nursing** (`HEALTHCARE`)

### Core Pillars
1. **Multi-Industry Master Profile:** Candidates choose their `JobCategory` specialization and record their contact details, headline, executive summary, verified skills, and chronological building blocks (work experience, education, certifications, and projects).
2. **Multi-Source Autonomous Job Ingestion:** Background workers periodically fetch real-world opportunities from public, zero-auth APIs (**JobTech Nordics**, **The Muse**, **Remotive**), sanitize HTML, and tag incoming jobs using industry-specific keyword dictionaries.
3. **Specialization-Aware Matching Engine:** Evaluates job openings against candidate profiles using a hybrid scoring algorithm combining explicit search preferences (40%) and semantic profile background overlap (60%).
4. **AI-Powered Tailoring:** On-demand generation of targeted, high-impact **Custom CVs** and **Cover Letters** via an LLM API (Google Gemini / OpenAI), complete with a resilient offline template fallback for zero-crash grading.
5. **Interactive Editor & Dynamic PDF Export:** Users review, edit, and fine-tune their application materials directly in an in-browser split-view workspace, then download production-ready PDF documents via OpenPDF.

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
│   ├── JobCategory.java            # Enum: Multi-industry specializations & dictionaries
│   ├── MasterProfile.java          # JPA Entity: Central career profile with specialization
│   ├── MasterProfileRepository.java
│   ├── ProfileItem.java            # JPA Entity: Experience, Education, Projects
│   ├── ProfileItemType.java        # Enum: EXPERIENCE, EDUCATION, CERTIFICATION, PROJECT
│   ├── ProfileItemRepository.java
│   ├── JobPosting.java             # JPA Entity: Ingested job listings with category
│   ├── JobPostingRepository.java
│   ├── ApplicationKit.java         # JPA Entity: Tailored CV/Cover letter artifacts
│   ├── ApplicationKitRepository.java
│   └── dto/                        # Transfer models (JobMatchDto, etc.) within domain
│
├── service/                        # Business Logic & External Integrations
│   ├── ProfileService.java         # Profile & resume item management
│   ├── JobScraperService.java      # Scheduled & manual multi-source job ingestion
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
- **Templating & UI:** Thymeleaf, HTML5, CSS3, Modern Responsive UI (Tailwind CSS CDN)
- **Database (Development):** In-Memory H2 Database (`jdbc:h2:mem:jobappdb`) with H2 Web Console
- **Database (Production / Grade 5):** PostgreSQL (`org.postgresql.Driver`)
- **PDF Generation:** OpenPDF (`com.github.librepdf:openpdf`)
- **External Public APIs:** JobTech Nordics API, The Muse API, Remotive API, Arbeitnow API
- **AI / LLM Integration:** Spring `RestClient` (calling Google Gemini / OpenAI API with deterministic offline fallback)
- **Testing:** JUnit 5, Mockito, Spring Boot Test (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`)
- **Build Tool:** Maven

---

## 4. Multi-Source Job Scraper & Ingestion Engine

The platform features an autonomous, multi-industry ingestion service implemented in [`JobScraperService`](src/main/java/dev/toluwalase/jobapp/service/JobScraperService.java). It automatically ingests real-world market postings across all career categories from four public, zero-auth APIs and persists them into the H2 database (`job_postings` table).

### 4.1 Integrated Public Job APIs (Zero-Auth)
1. **JobTech OpenAPI (Nordics / Multi-Industry):**
   - **Base URL:** `https://jobsearch.api.jobtechdev.se/search`
   - **Method:** `GET /search?q={query}&limit=12`
   - **Coverage:** Public open data covering all industries across Nordic municipalities (technology, cleaning/facility operations, legal, marketing, finance, healthcare).
   - **Key Fields:** `id` -> `headline` (title), `employer.name` (company), `workplace_address.municipality` (location), `description.text` (body), `webpage_url` (source link).

2. **The Muse Public API (Global / Multi-Industry):**
   - **Base URL:** `https://www.themuse.com/api/public/jobs`
   - **Method:** `GET /jobs?category={category}&page=1`
   - **Coverage:** Global white-collar, corporate, engineering, marketing, and legal roles.
   - **Resilience:** Requests include realistic browser headers and gracefully handle Cloudflare anti-bot challenges (`cf-mitigated`) without crashing the application.
   - **Key Fields:** `id` -> `name` (title), `company.name`, `locations[0].name`, `contents` (HTML body), `refs.landing_page`.

3. **Remotive API (Global Remote):**
   - **Base URL:** `https://remotive.com/api/remote-jobs`
   - **Method:** `GET /remote-jobs?category={slug}&limit=10`
   - **Coverage:** Worldwide remote opportunities (`software-dev`, `marketing`, `finance-legal`, `all-others`).
   - **Key Fields:** `id` -> `title`, `company_name`, `candidate_required_location` (remote designation), `description` (HTML body), `url`.

4. **Arbeitnow API (European Multi-Industry Feed):**
   - **Base URL:** `https://www.arbeitnow.com/api/job-board-api`
   - **Method:** `GET /job-board-api`
   - **Coverage:** Broad European job portal containing diverse tech, sales, marketing, operations, and support roles.
   - **Dynamic Categorization:** General listings are dynamically classified into the optimal `JobCategory` (`determineCategory`) using weighted title, tag, and keyword matching.
   - **Key Fields:** `slug` (unique slug ID) -> `title`, `company_name`, `location`, `description` (HTML body), `tags` array, `url`.

### 4.2 Ingestion & Data Processing Pipeline
Each retrieved listing passes through a resilient, 5-stage processing pipeline:

```mermaid
flowchart TD
    API["External API Payload"] --> ID["Generate Prefixed External ID<br/>(jobtech-*, themuse-*, remotive-*, arbeitnow-*)"]
    ID --> DUP{"existsByExternalId?"}
    DUP -- "Yes" --> SKIP["Skip (Prevent Duplicate Row)"]
    DUP -- "No" --> SANITIZE["cleanHtml()<br/>Strip HTML tags, decode entities, normalize spaces"]
    SANITIZE --> TAGS["extractTags()<br/>Regex word-boundary match against JobCategory dictionary"]
    TAGS --> PERSIST["jobPostingRepository.save(posting)<br/>Persist JobPosting into H2 database"]
```

1. **Source-Prefixed Identifiers:** To eliminate collisions across different APIs, external IDs are prefixed by source: `jobtech-{id}`, `themuse-{id}`, `remotive-{id}`, and `arbeitnow-{slug}`.
2. **Deduplication Check:** Calls `jobPostingRepository.existsByExternalId(...)` before parsing or inserting. Existing listings are skipped instantly.
3. **HTML Sanitization (`cleanHtml`):** Strips block and inline tags (`<p>`, `<div>`, `<br/>`, `<strong>`), decodes HTML entities (`&amp;`, `&quot;`, `&#39;`, `&nbsp;`, `&#8211;`), fixes punctuation spacing, and collapses redundant whitespace.
4. **Domain Keyword Tagging (`extractTags`):** Cross-references the job's title and description against the specific `JobCategory`'s keyword dictionary using regex word boundaries (preventing false positives like matching `Java` inside `JavaScript`).
5. **Persistence:** Saves the populated `JobPosting` entity with its category, clean description, normalized tags, source metadata, and scraping timestamp.

### 4.3 Trigger Modes
- **Scheduled Automated Ingestion:** Powered by Spring's `@Scheduled(cron = "0 0 * * * *")` running hourly in the background (enabled via `@EnableScheduling` in `JobappApplication`).
- **Synchronous Programmatic Trigger:** `jobScraperService.triggerScrapeNow()` executes full multi-source ingestion on demand and returns the count of newly saved records.
- **Web UI Trigger:** A *"Fetch Latest Jobs Now"* button in the web interface calls `POST /jobs/scrape` and redirects back to `/jobs` with a flash confirmation message.

### 4.4 Zero-Crash Resilience Guarantee
- Every external API invocation is isolated in individual `try-catch` blocks with explicit network timeouts (6s connect, 10s read).
- If any external endpoint is unreachable, offline, rate-limited, or blocked by bot challenges, the service logs a warning and gracefully continues scraping remaining providers.
- Comprehensive unit tests in `JobScraperServiceTest` verify complete offline operation with 0 unhandled exceptions.

---

## 5. Two-Tier Job Match Scoring Engine

The platform features an intelligent, specialization-aware matching engine implemented in [`MatchingEngineService`](src/main/java/dev/toluwalase/jobapp/service/MatchingEngineService.java) and encapsulated in [`JobMatchDto`](src/main/java/dev/toluwalase/jobapp/domain/JobMatchDto.java). It scores each job posting against a candidate's [`MasterProfile`](src/main/java/dev/toluwalase/jobapp/domain/MasterProfile.java) on an objective **0–100 scale**.

### 5.1 The Scoring Formula

```
Raw Match Score (0–100) = Tier 1: Preferences (0–40) + Tier 2: Profile Evidence (0–60)
```

$$\text{Final Score} = \begin{cases} \text{Raw Score}, & \text{if job category matches candidate specialization} \\ \text{round}(\text{Raw Score} \times 0.60), & \text{if cross-industry (40\% penalty)} \end{cases}$$

```mermaid
flowchart TD
    subgraph T1["Tier 1: Explicit Preferences (Max 40 pts)"]
        LOC["Location Match (0–20 pts)<br/>Exact city, municipality, or remote status"]
        ROLES["Target Role Tags (0–20 pts)<br/>Candidate preferred role keywords vs. job content"]
    end

    subgraph T2["Tier 2: Profile Evidence (Max 60 pts)"]
        SKILLS["Skills Overlap (0–40 pts)<br/>Candidate skillsList vs. job extractedTags & body"]
        EXP["Background Evidence (0–20 pts)<br/>Work experience, certifications, education, & projects"]
    end

    T1 --> RAW["Raw Composite Score (0–100)"]
    T2 --> RAW
    RAW --> SPEC{"Specialization Match?"}
    SPEC -- "Yes (Same Industry)" --> FINAL["Final Score (100% of Raw)"]
    SPEC -- "No (Different Industry)" --> PENALTY["Final Score (60% of Raw — 40% Penalty)"]
```

---

### 5.2 Tier 1: Explicit Candidate Preferences (Max 40 Points)

Evaluates whether the job opening satisfies the candidate's explicit search criteria:

1. **Location Alignment (`0–20 points`):**
   - Matches candidate's comma-separated `preferredLocations` (e.g. `"Helsinki, Espoo, Remote"`) against `job.location`.
   - **Remote Compatibility:** If the candidate lists `"Remote"` and the job contains `"Remote"`, `"Hybrid"`, or `"Worldwide"`, full **20 points** are awarded with a `"Location Match: Remote / Hybrid"` rationale.
   - **City / Region Match:** If any preferred city appears in the job location, **20 points** are awarded.
   - **Neutral Default:** If the candidate left preferred locations blank, **12 points** are awarded by default so they aren't penalized.

2. **Role & Domain Tag Preference (`0–20 points`):**
   - Matches candidate's `preferredTags` (e.g. `"Backend, Software Engineer, Cloud, Junior"`) against the job's title, tags, and description using regex word boundaries:
     ```java
     Pattern pattern = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9])" + Pattern.quote(tag) + "(?=[^a-zA-Z0-9]|$)");
     ```
   - Score is proportional to the fraction of preferred tags found:
     $$\text{Tag Score} = \text{round}\left(\frac{\text{matched tags}}{\text{total preferred tags}} \times 20.0\right)$$

---

### 5.3 Tier 2: Profile Evidence Overlap (Max 60 Points)

Evaluates whether the candidate has demonstrable proof of competence in their profile:

1. **Skills & Job Tag Overlap (`0–40 points`):**
   - Compares candidate's `skillsList` (e.g. `"Java, Spring Boot, PostgreSQL, Docker, AWS"`) against the job's normalized `extractedTags`:
     ```java
     // Ratio of matched required job tags
     double ratio = (double) matchedTags / jobTags.size();
     int score = (int) Math.round(ratio * 35.0);

     // Bonus for candidate skills found directly in the job description text
     for (String skill : candidateSkills) {
         if (jobTextMatches(skill)) {
             score += 3; // +3 pts per extra skill mentioned in description
         }
     }
     return Math.min(40, score);
     ```
   - **Fallback for Un-tagged Jobs:** If a scraped job has no extracted tags, candidate skills are searched directly across the job title and description at **8 points per matched skill** (capped at 40).

2. **Background & Building Block Evidence (`0–20 points`):**
   - Scans the candidate's chronological [`ProfileItem`](src/main/java/dev/toluwalase/jobapp/domain/ProfileItem.java) entries:
     - **Work Experience (`EXPERIENCE`, +5 pts each, max 12):** Awarded when past job titles or descriptions mention the target role title or required tech stack.
     - **Certifications (`CERTIFICATION`, +5 pts):** Awarded when the certification title matches indexed job keywords (e.g. `"AWS Certified Cloud Practitioner"` matching an AWS requirement).
     - **Education (`EDUCATION`, +3 pts):** Awarded for verified academic credentials.
     - **Projects (`PROJECT`, +3 pts each, max 6):** Awarded when candidate project descriptions demonstrate relevant tools.
     - **Career Summary (+2 pts):** Awarded when the candidate's executive bio contains matching keywords.

---

### 5.4 Specialization Alignment & Cross-Industry Penalty

To prioritize relevant openings while still surfacing transferable career opportunities:
- **Matching Specialization:** If `job.category == profile.specialization`, the candidate receives 100% of their raw score and a top-level `"Specialization Match: {Category}"` badge.
- **Cross-Industry Opening:** If the candidate is specialized in `TECHNOLOGY` but views a `SERVICE_OPERATIONS` or `MARKETING` listing, a **40% penalty** is applied ($\text{Raw Score} \times 0.60$) and a `"Cross-Industry Opportunity"` badge is attached.

---

### 5.5 Transparent Algorithm Rationale (`matchReasons`)

Every point awarded generates human-readable rationale pills displayed directly on the job card:
- 🔵 `Specialization Match: Technology & IT`
- 🟢 `Skill Match: Java`
- 🟢 `Skill Match: Spring Boot`
- 🟢 `Skill Match: Docker`
- 🌐 `Location Match: Helsinki`
- 🟣 `Experience: Junior Backend Developer`
- 🟣 `Certification: AWS Certified Cloud Practitioner`
- 🟡 `Target Role Tag: Backend`

---

## 6. AI Application Tailoring & Dynamic PDF Export (Milestone 5)

JobApp provides an automated application workshop that creates personalized CVs and cover letters targeted at specific job openings, complete with side-by-side editing and instantaneous PDF generation.

```
       [ Candidate Profile ]               [ Job Posting ]
                 │                                │
                 └───────────────┬────────────────┘
                                 ▼
                         TailoringService
                 ┌───────────────┴───────────────┐
                 │                               │
        (API Key Available)             (No Key / Offline)
                 │                               │
                 ▼                               ▼
         Live Gemini LLM            Deterministic Fallback
      (gemini-1.5-flash API)        (Candidate Data + Tags)
                 │                               │
                 └───────────────┬───────────────┘
                                 ▼
                     ApplicationKit Entity
                 (tailoredCvContent, tailoredLetter)
                                 │
                 ┌───────────────┴───────────────┐
                 ▼                               ▼
       Split-Screen Workspace             PdfExportService
         (/kits/{id}/edit)                   (OpenPDF)
                 │                               │
       Interactive HTML Editor           Downloadable PDF Stream
         (Save Draft Edits)         (/kits/{id}/pdf/cv & /cover-letter)
```

### 6.1 Dual-Mode Tailoring Architecture (`TailoringService`)

To balance cutting-edge AI capabilities with 100% test reliability and zero-crash evaluation, the `TailoringService` implements a dual-mode generation strategy:

1. **Live Gemini LLM Inference Mode:**
   - Active when `${gemini.api.key}` or `GEMINI_API_KEY` is present.
   - Dispatches a prompt structured with candidate experience, skills, specialization context, and target job description to `gemini-1.5-flash` via Spring `RestClient`.
   - Generates customized ATS-friendly Markdown CVs and formal business cover letters separated by `===COVER_LETTER===`.
2. **Deterministic Offline Template Mode (Zero-Crash Fallback):**
   - Active when no API key is set or when external network calls fail.
   - Synthesizes structured markdown CVs and formal letters directly from `MasterProfile` items (`EXPERIENCE`, `EDUCATION`, `CERTIFICATION`, `PROJECT`) and the job's `extractedTags`.
   - Caches generated kits by `(userId, jobId)` in `ApplicationKitRepository` so previous revisions are preserved.

### 6.2 Split-Screen Workspace UI (`kit-editor.html`)

Accessing `/kits/{kitId}/edit` opens a split-screen editor:
- **Left Column:** Target job requirements, company details, location, category pill, 0–100 match score breakdown, rationale pills, and full job description.
- **Right Column:** Tabbed interface switching between **Tailored CV (Markdown)** and **Tailored Cover Letter (Business Correspondence)** with live inline text editing.
- **Action Toolbar:** One-click save (`POST /kits/{kitId}/save`) with flash feedback and instant PDF export buttons.

### 6.3 Dynamic PDF Export (`PdfExportService`)

Built with OpenPDF (`com.github.librepdf:openpdf`), `PdfExportService` converts draft text directly into downloadable binary PDF streams:
- `GET /kits/{kitId}/pdf/cv`: Generates a clean A4 curriculum vitae with section dividers, bulleted skills, bold job titles, and italicized dates.
- `GET /kits/{kitId}/pdf/cover-letter`: Formats a formal business letter with date header, recipient address block, salutation, justified body paragraphs, and candidate signature block.
- Streams responses with `Content-Type: application/pdf` and sanitized filename headers (e.g. `attachment; filename="CV-Senior_Java_Developer.pdf"`).

---

## 7. Getting Started

### Prerequisites
- Java 21 SDK installed (`java -version`)
- Maven 3.9+ (or use the included `./mvnw` wrapper)

### Running the Application Locally
1. Clone the repository and navigate to the project directory:
   ```bash
   cd /home/zappy/Work/job-app
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

## 8. Course & Grading Compliance (Grade 5 Target)

- [x] **Strict 3-Package Architecture:** Enforces separation of concerns (`domain`, `service`, `web`).
- [x] **No Lombok:** Handwritten, clean, standard constructors, getters, and setters.
- [x] **Comprehensive Documentation:** Full Javadoc on all classes and methods.
- [x] **Multi-Industry Specialization:** Support for Tech, Marketing, Legal, Operations/Cleaning, Finance, and Healthcare.
- [x] **Multi-Source Scheduled Scraper:** Hourly background ingestion from JobTech Nordics, The Muse, Remotive, and Arbeitnow with manual trigger.
- [x] **Two-Tier Matching Algorithm:** 40% preference overlap + 60% semantic profile evidence score.
- [x] **Dual-Mode AI Tailoring:** Live LLM integration via `RestClient` + deterministic offline template fallback.
- [x] **Dynamic PDF Generation:** On-the-fly binary rendering of tailored CVs and cover letters via OpenPDF.
- [ ] **Dual Web + REST Architecture:** Thymeleaf server-rendered interface alongside `/api/v1/**` REST API.
- [ ] **Automated Test Suite:** Unit tests for matching algorithms, Mockito mocks for external APIs, and JPA slice tests.
