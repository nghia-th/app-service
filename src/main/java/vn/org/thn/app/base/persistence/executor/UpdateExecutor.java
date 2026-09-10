package vn.org.thn.app.base.persistence.executor;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;


import java.util.Map;
import java.util.Set;

/**
 * Low-level "update this entity's row by its @Id column(s)" helper, one level below the
 * {@code UpdateBuilder} DSL - mirrors the Kotlin original (there it was actually unreachable,
 * declared {@code private} with no caller; made public here since it is a reasonable building
 * block for a repository that already has an entity + a partial column set in hand).
 */
@Component
public class UpdateExecutor extends BaseExecutor {

    public UpdateExecutor(SqlSessionTemplate session) {
        super(session);
    }

    /** Updates {@code columns} on the one row whose id column(s) match {@code paramMap}'s values for {@code info.getIds()}. */
    public void update(Set<String> columns, EntityInfo info, Map<String, Object> paramMap) {
        String setClause = String.join(", ", columns.stream().map(c -> c + " = #{" + c + "}").toList());
        String where = String.join(" AND ", info.getIds().stream().map(c -> c + " = #{" + c + "}").toList());
        runSql("UPDATE " + info.getTableName() + " SET " + setClause + " WHERE " + where, paramMap);
    }
}
