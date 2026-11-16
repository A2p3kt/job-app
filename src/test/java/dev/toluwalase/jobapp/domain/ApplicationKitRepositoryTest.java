package dev.toluwalase.jobapp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Data JPA slice test verifying custom query methods in
 * ApplicationKitRepository.
 */
@DataJpaTest
public class ApplicationKitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApplicationKitRepository kitRepository;

    @BeforeEach
    void setUp() {
        kitRepository.deleteAll();
    }

    @Test
    @DisplayName("findByUserIdAndJobPostingId retrieves unique kit pairing")
    void testFindByUserIdAndJobPostingId() {
        AppUser user = new AppUser("tester", "tester@example.com", "hash", "LOCAL", "ROLE_USER");
        entityManager.persist(user);

        JobPosting job = new JobPosting(
                "jobtech-pairing-1",
                JobCategory.TECHNOLOGY,
                "DevOps Engineer",
                "Cloud Solutions",
                "Remote",
                "Kubernetes and Terraform",
                "Docker, Kubernetes",
                "https://example.com/job/pairing",
                "JobTech",
                LocalDateTime.now());
        entityManager.persist(job);

        ApplicationKit kit = new ApplicationKit(
                user,
                job,
                88,
                "Docker, Kubernetes match",
                "# CV",
                "Dear Hiring Manager",
                "DRAFT");
        entityManager.persistAndFlush(kit);

        Optional<ApplicationKit> found = kitRepository.findByUserIdAndJobPostingId(user.getId(), job.getId());
        assertTrue(found.isPresent());
        assertEquals(88, found.get().getMatchScore());
        assertEquals("Docker, Kubernetes match", found.get().getMatchReasons());

        Optional<ApplicationKit> notFound = kitRepository.findByUserIdAndJobPostingId(user.getId(), 9999L);
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("findByUserIdOrderByUpdatedAtDesc returns user's kits chronologically")
    void testFindByUserIdOrderByUpdatedAtDesc() {
        AppUser user = new AppUser("user2", "user2@example.com", "hash", "LOCAL", "ROLE_USER");
        entityManager.persist(user);

        JobPosting job1 = new JobPosting("job-1", JobCategory.TECHNOLOGY, "Dev 1", "Co", "Loc", "Desc", "Java", "url",
                "JobTech", LocalDateTime.now());
        JobPosting job2 = new JobPosting("job-2", JobCategory.TECHNOLOGY, "Dev 2", "Co", "Loc", "Desc", "Java", "url",
                "JobTech", LocalDateTime.now());
        entityManager.persist(job1);
        entityManager.persist(job2);

        ApplicationKit kit1 = new ApplicationKit(user, job1, 75, "Reason", "CV 1", "Letter 1", "DRAFT");
        kit1.setUpdatedAt(LocalDateTime.now().minusHours(2));

        ApplicationKit kit2 = new ApplicationKit(user, job2, 90, "Reason", "CV 2", "Letter 2", "FINALIZED");
        kit2.setUpdatedAt(LocalDateTime.now().minusHours(1));

        entityManager.persist(kit1);
        entityManager.persist(kit2);
        entityManager.flush();
        entityManager.clear();

        List<ApplicationKit> kits = kitRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        assertEquals(2, kits.size());
        assertEquals(90, kits.get(0).getMatchScore());
        assertEquals(75, kits.get(1).getMatchScore());
    }
}
