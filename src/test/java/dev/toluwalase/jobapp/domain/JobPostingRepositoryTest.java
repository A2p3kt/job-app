package dev.toluwalase.jobapp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Data JPA slice test verifying custom query methods in JobPostingRepository.
 */
@DataJpaTest
public class JobPostingRepositoryTest {

        @Autowired
        private TestEntityManager entityManager;

        @Autowired
        private JobPostingRepository jobPostingRepository;

        @Autowired
        private ApplicationKitRepository applicationKitRepository;

        @BeforeEach
        void setUp() {
                applicationKitRepository.deleteAll();
                jobPostingRepository.deleteAll();
        }

        @Test
        @DisplayName("existsByExternalId returns true for existing postings and false otherwise")
        void testExistsByExternalId() {
                JobPosting posting = new JobPosting(
                                "jobtech-1001",
                                JobCategory.TECHNOLOGY,
                                "Backend Engineer",
                                "Acme Corp",
                                "Helsinki",
                                "Java Spring Boot developer role.",
                                "Java, Spring Boot",
                                "https://example.com/job/1001",
                                "JobTech",
                                LocalDateTime.now());
                entityManager.persistAndFlush(posting);

                assertTrue(jobPostingRepository.existsByExternalId("jobtech-1001"));
                assertFalse(jobPostingRepository.existsByExternalId("non-existent-id"));
        }

        @Test
        @DisplayName("findByCategoryOrderByScrapedAtDesc filters by category with latest first")
        void testFindByCategoryOrderByScrapedAtDesc() {
                JobPosting tech1 = new JobPosting(
                                "jobtech-tech-1",
                                JobCategory.TECHNOLOGY,
                                "Junior Dev",
                                "Tech AB",
                                "Stockholm",
                                "Description",
                                "Java",
                                "https://example.com/1",
                                "JobTech",
                                LocalDateTime.now().minusHours(2));
                JobPosting tech2 = new JobPosting(
                                "jobtech-tech-2",
                                JobCategory.TECHNOLOGY,
                                "Senior Dev",
                                "Tech AB",
                                "Stockholm",
                                "Description",
                                "Java, AWS",
                                "https://example.com/2",
                                "JobTech",
                                LocalDateTime.now().minusHours(1));
                JobPosting health = new JobPosting(
                                "jobtech-health-1",
                                JobCategory.HEALTHCARE,
                                "Nurse",
                                "Care AB",
                                "Gothenburg",
                                "Description",
                                "Patient Care",
                                "https://example.com/3",
                                "JobTech",
                                LocalDateTime.now());

                entityManager.persist(tech1);
                entityManager.persist(tech2);
                entityManager.persist(health);
                entityManager.flush();
                entityManager.clear();

                List<JobPosting> techJobs = jobPostingRepository
                                .findByCategoryOrderByScrapedAtDesc(JobCategory.TECHNOLOGY);
                assertEquals(2, techJobs.size());
                assertEquals("jobtech-tech-2", techJobs.get(0).getExternalId());
                assertEquals("jobtech-tech-1", techJobs.get(1).getExternalId());
        }
}
