package vn.org.thn.app.base.persistence;

import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.org.thn.app.base.persistence.annotation.Column;
import vn.org.thn.app.base.persistence.annotation.Entity;
import vn.org.thn.app.base.persistence.annotation.GeneratedValue;
import vn.org.thn.app.base.persistence.annotation.GenerationType;
import vn.org.thn.app.base.persistence.annotation.Id;
import vn.org.thn.app.base.persistence.annotation.Table;
import vn.org.thn.app.base.persistence.annotation.Unaccent;
import vn.org.thn.app.base.persistence.executor.BatchInsertExecutor;
import vn.org.thn.app.base.persistence.executor.InsertExecutor;
import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.persistence.query.QueryBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UnaccentIntegrationTest {

    @Data
    @Entity
    @Table(name = "tbl_test_unaccent")
    public static class TestPersonEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Long id;

        @Column(name = "name")
        private String name;

        @Unaccent(from = "name")
        @Column(name = "name_unaccent")
        private String nameUnaccent;
    }

    @Autowired
    private SqlSessionTemplate session;

    @Autowired
    private InsertExecutor insertExecutor;

    @Autowired
    private BatchInsertExecutor batchInsertExecutor;

    @Autowired
    private QueryExecutor queryExecutor;

    private EntityInfo info;

    @BeforeEach
    void setUp() {
        session.update("DynamicSQL.execute", Map.of("sql",
                "CREATE TABLE IF NOT EXISTS tbl_test_unaccent (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, " +
                        "name_unaccent TEXT)"));
        info = EntityCache.get(TestPersonEntity.class);
    }

    @AfterEach
    void tearDown() {
        session.update("DynamicSQL.execute", Map.of("sql", "DROP TABLE IF EXISTS tbl_test_unaccent"));
    }

    @Test
    @DisplayName("Should auto-populate @Unaccent field on INSERT and UPDATE")
    void testAutoPopulateUnaccentField() {
        TestPersonEntity person = new TestPersonEntity();
        person.setName("Trương Hiếu Nghĩa");
        assertNull(person.getNameUnaccent());

        // 1. Test INSERT
        TestPersonEntity saved = insertExecutor.save(person);
        assertNotNull(saved.getId());
        assertEquals("truong hieu nghia", saved.getNameUnaccent());

        // 2. Test UPDATE
        saved.setName("Đặng Văn Lâm");
        TestPersonEntity updated = insertExecutor.save(saved);
        assertEquals("dang van lam", updated.getNameUnaccent());
    }

    @Test
    @DisplayName("Should search in any word order using likeAnyOrder (Approach 1)")
    void testSearchLikeAnyOrder() {
        TestPersonEntity person = new TestPersonEntity();
        person.setName("Trương Hiếu Nghĩa");
        insertExecutor.save(person);

        // Permutation 1: "HIếu Nghĩa Trương"
        List<TestPersonEntity> r1 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrder(TestPersonEntity::getName, "HIếu Nghĩa Trương")
                .list();
        assertEquals(1, r1.size());
        assertEquals("Trương Hiếu Nghĩa", r1.get(0).getName());

        // Permutation 2: "Nghĩa Trương Hiếu"
        List<TestPersonEntity> r2 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrder(TestPersonEntity::getName, "Nghĩa Trương Hiếu")
                .list();
        assertEquals(1, r2.size());

        // Permutation 3: "Nghĩa Hiếu Trương"
        List<TestPersonEntity> r3 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrder(TestPersonEntity::getName, "Nghĩa Hiếu Trương")
                .list();
        assertEquals(1, r3.size());

        // Partial match: "Trương Nghĩa"
        List<TestPersonEntity> r4 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrder(TestPersonEntity::getName, "Trương Nghĩa")
                .list();
        assertEquals(1, r4.size());

        // Negative match: word not present
        List<TestPersonEntity> r5 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrder(TestPersonEntity::getName, "Trương Tuấn")
                .list();
        assertEquals(0, r5.size());
    }

    @Test
    @DisplayName("Should search unaccented in any word order using likeAnyOrderUnaccent (Approach 2)")
    void testSearchLikeAnyOrderUnaccent() {
        TestPersonEntity person = new TestPersonEntity();
        person.setName("Trương Hiếu Nghĩa");
        insertExecutor.save(person);

        // Search with no accents, different order: "hieu nghia truong"
        List<TestPersonEntity> r1 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrderUnaccent(TestPersonEntity::getNameUnaccent, "hieu nghia truong")
                .list();
        assertEquals(1, r1.size());
        assertEquals("Trương Hiếu Nghĩa", r1.get(0).getName());

        // Search with partial no accents: "truong nghia"
        List<TestPersonEntity> r2 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrderUnaccent(TestPersonEntity::getNameUnaccent, "truong nghia")
                .list();
        assertEquals(1, r2.size());

        // Search when user inputs accented keyword into likeAnyOrderUnaccent: "Nghĩa Hiếu Trương"
        List<TestPersonEntity> r3 = new QueryBuilder<>(TestPersonEntity.class, info, queryExecutor)
                .likeAnyOrderUnaccent(TestPersonEntity::getNameUnaccent, "Nghĩa Hiếu Trương")
                .list();
        assertEquals(1, r3.size());
    }

    @Test
    @DisplayName("Should auto-populate @Unaccent fields in batch saveAll")
    void testBatchSaveAllPopulatesUnaccent() {
        TestPersonEntity p1 = new TestPersonEntity();
        p1.setName("Nguyễn Văn A");
        TestPersonEntity p2 = new TestPersonEntity();
        p2.setName("Trần Thị B");

        List<TestPersonEntity> savedList = batchInsertExecutor.saveAll(List.of(p1, p2));
        assertEquals(2, savedList.size());
        assertEquals("nguyen van a", savedList.get(0).getNameUnaccent());
        assertEquals("tran thi b", savedList.get(1).getNameUnaccent());
    }
}
