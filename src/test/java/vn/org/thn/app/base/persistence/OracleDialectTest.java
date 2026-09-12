package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.org.thn.app.base.persistence.dialect.OracleDialect;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for OracleDialect's identity-retrieval SQL generation. */
class OracleDialectTest {

    private final OracleDialect dialect = new OracleDialect();

    @Test
    @DisplayName("buildInsertReturningCallable produces a RETURNING ... INTO block bound to RETURNING_ID_PARAM")
    void buildInsertReturningCallable_withIdentityColumn_bindsOutParameter() {
        String sql = dialect.buildInsertReturningCallable("tbl_user", "username,email", "#{username},#{email}", "id");

        assertEquals(
                "BEGIN INSERT INTO tbl_user (username,email) VALUES (#{username},#{email}) RETURNING id INTO "
                        + "#{" + SqlDialect.RETURNING_ID_PARAM + ", mode=OUT, jdbcType=NUMERIC}; END;",
                sql
        );
    }

    @Test
    @DisplayName("buildInsertReturningCallable returns null when there is no identity column")
    void buildInsertReturningCallable_withoutIdentityColumn_returnsNull() {
        assertNull(dialect.buildInsertReturningCallable("tbl_user", "username", "#{username}", null));
    }

    @Test
    @DisplayName("singleStatementReturning is false - Oracle never uses the embedded-RETURNING single-statement path")
    void singleStatementReturning_isFalse() {
        assertFalse(dialect.singleStatementReturning());
    }

    @Test
    @DisplayName("buildIdentitySelect (the old, unsafe-under-concurrency fallback) is still available but not used by insertReturningId when a callable path exists")
    void buildIdentitySelect_stillBuildsSelectMax() {
        assertEquals("SELECT MAX(id) AS id FROM tbl_user", dialect.buildIdentitySelect("tbl_user", "id"));
    }
}
