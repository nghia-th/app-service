package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.org.thn.app.base.core.context.UserContext;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class BaseEntityAuditIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("Auto-audit on INSERT defaults to 'system' and current timestamp")
    void testAutoAuditOnInsertDefault() {
        UserEntity user = new UserEntity();
        user.setUsername("audit_insert_" + System.currentTimeMillis());
        user.setEmail(user.getUsername() + "@test.com");
        user.setFullName("Audit Default User");

        UserEntity saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt(), "createdAt should be auto-populated");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-populated");
        assertEquals("system", saved.getCreatedBy(), "createdBy should default to system");
        assertEquals("system", saved.getUpdatedBy(), "updatedBy should default to system");
        assertFalse(saved.isDeleted(), "deleted should default to false");

        // Verify persisted state in DB
        UserEntity fromDb = userRepository.findById(saved.getId());
        assertNotNull(fromDb);
        assertEquals("system", fromDb.getCreatedBy());
        assertEquals("system", fromDb.getUpdatedBy());
        assertNotNull(fromDb.getCreatedAt());
        assertNotNull(fromDb.getUpdatedAt());
    }

    @Test
    @DisplayName("Auto-audit on INSERT picks up username from UserContext")
    void testAutoAuditWithUserContext() {
        UserContext.setCurrentUser("nghia.truong");

        UserEntity user = new UserEntity();
        user.setUsername("audit_ctx_" + System.currentTimeMillis());
        user.setEmail(user.getUsername() + "@test.com");
        user.setFullName("Nghia Truong");

        UserEntity saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("nghia.truong", saved.getCreatedBy());
        assertEquals("nghia.truong", saved.getUpdatedBy());

        UserEntity fromDb = userRepository.findById(saved.getId());
        assertNotNull(fromDb);
        assertEquals("nghia.truong", fromDb.getCreatedBy());
        assertEquals("nghia.truong", fromDb.getUpdatedBy());
    }

    @Test
    @DisplayName("Auto-audit on UPDATE updates updatedAt/updatedBy while protecting original createdAt/createdBy")
    void testAutoAuditOnUpdateProtectsCreatedAtAndUpdatesUpdatedAt() throws InterruptedException {
        UserContext.setCurrentUser("original.creator");

        UserEntity user = new UserEntity();
        user.setUsername("audit_update_" + System.currentTimeMillis());
        user.setEmail(user.getUsername() + "@test.com");
        user.setFullName("Initial Name");

        UserEntity saved = userRepository.save(user);
        Long id = saved.getId();
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        String originalCreatedBy = saved.getCreatedBy();

        assertEquals("original.creator", originalCreatedBy);
        assertNotNull(originalCreatedAt);

        // Sleep briefly to ensure timestamp difference
        Thread.sleep(50);

        // Switch user context to simulate another user updating the record
        UserContext.setCurrentUser("updater.user");

        saved.setFullName("Updated Name");
        UserEntity updated = userRepository.save(saved);

        assertEquals("Updated Name", updated.getFullName());
        assertEquals("original.creator", updated.getCreatedBy(), "createdBy must NOT change on UPDATE");
        assertEquals("updater.user", updated.getUpdatedBy(), "updatedBy must reflect the new user");
        assertEquals(originalCreatedAt, updated.getCreatedAt(), "createdAt must NOT change on UPDATE");
        assertTrue(updated.getUpdatedAt().isAfter(originalCreatedAt) || updated.getUpdatedAt().isEqual(originalCreatedAt));

        // Verify from DB directly
        UserEntity fromDb = userRepository.findById(id);
        assertNotNull(fromDb);
        assertEquals("Updated Name", fromDb.getFullName());
        assertEquals("original.creator", fromDb.getCreatedBy(), "DB created_by must remain original");
        assertEquals("updater.user", fromDb.getUpdatedBy(), "DB updated_by must be updated");
        assertEquals(originalCreatedAt, fromDb.getCreatedAt(), "DB created_at must remain original");
    }

    @Test
    @DisplayName("Batch saveAll auto-populates audit fields for all BaseEntity items")
    void testBatchSaveAllPopulatesAudit() {
        UserContext.setCurrentUser("batch.worker");

        UserEntity u1 = new UserEntity();
        u1.setUsername("batch_u1_" + System.currentTimeMillis());
        u1.setEmail(u1.getUsername() + "@test.com");

        UserEntity u2 = new UserEntity();
        u2.setUsername("batch_u2_" + System.currentTimeMillis());
        u2.setEmail(u2.getUsername() + "@test.com");

        List<UserEntity> list = userRepository.saveAll(List.of(u1, u2));

        assertEquals(2, list.size());
        for (UserEntity u : list) {
            assertNotNull(u.getId());
            assertNotNull(u.getCreatedAt());
            assertNotNull(u.getUpdatedAt());
            assertEquals("batch.worker", u.getCreatedBy());
            assertEquals("batch.worker", u.getUpdatedBy());
            assertFalse(u.isDeleted());
        }
    }
}
