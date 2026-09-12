package vn.org.thn.app.modules.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.persistence.query.QueryBuilder;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for Medium finding #13 (2026-09-12 review): UserEntity - the exact entity
 * docs/BASE_FRAMEWORK_GUIDE.md 2.8 and docs/PROMPT_TEMPLATES.md "Mau 2.3" use as the flagship
 * example for accent-insensitive, word-order-independent smart search - never actually declared
 * fullNameUnaccent/@Unaccent, so UserService#getUsersPaged's keyword search fell back to an exact
 * match on fullName instead of demonstrating the feature. This reproduces the exact query shape now
 * built by UserService#getUsersPaged and checks the permutation/diacritic cases the docs themselves
 * ask for ("Nghia Truong Hieu" -> "Truong Hieu Nghia").
 */
class UserSmartSearchQueryTest {

    private EntityInfo userEntityInfo;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp() {
        queryExecutor = Mockito.mock(QueryExecutor.class);
        userEntityInfo = EntityCache.get(UserEntity.class);
    }

    /** Mirrors the exact nested and/or expression in UserService#getUsersPaged. */
    private QueryBuilder<UserEntity> smartSearch(String keyword) {
        QueryBuilder<UserEntity> query = new QueryBuilder<>(UserEntity.class, userEntityInfo, queryExecutor);
        query.and(sub -> sub
                .like(UserEntity::getUsername, keyword)
                .orEq(UserEntity::getEmail, keyword)
                .or(nameSub -> nameSub.likeAnyOrderUnaccent(UserEntity::getFullNameUnaccent, keyword))
        );
        return query;
    }

    @Test
    @DisplayName("Keyword search matches full_name_unaccent regardless of word order")
    void smartSearch_wordOrderPermutation_bothMatch() {
        QueryBuilder<UserEntity> reordered = smartSearch("Nghĩa Trương Hiếu");

        String sql = reordered.toSql();
        assertTrue(sql.contains("full_name_unaccent LIKE #{"), "should search on the unaccented mirror column, not full_name directly");
        assertTrue(reordered.getParams().containsValue("%nghia%"));
        assertTrue(reordered.getParams().containsValue("%truong%"));
        assertTrue(reordered.getParams().containsValue("%hieu%"), "tokens must be unaccented+lowercased, matching how full_name_unaccent itself is populated");
    }

    @Test
    @DisplayName("The unaccented-name branch is OR-joined with username/email, not AND-joined")
    void smartSearch_nameBranch_isOrJoinedWithUsernameAndEmail() {
        QueryBuilder<UserEntity> query = smartSearch("nghia");

        String sql = query.toSql();
        assertTrue(sql.contains("username LIKE #{"));
        assertTrue(sql.contains("OR email = #{"));
        assertTrue(sql.contains("OR (full_name_unaccent LIKE #{"),
                "the nested likeAnyOrderUnaccent group must fold in as one OR-joined parenthesized fragment");
    }
}
