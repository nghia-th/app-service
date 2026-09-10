package vn.org.thn.app.base.persistence.executor;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;


import java.util.Map;

/** Low-level "delete this entity's row by its @Id column(s)" helper, one level below {@code DeleteBuilder}. */
@Component
public class DeleteExecutor extends BaseExecutor {

    public DeleteExecutor(SqlSessionTemplate session) {
        super(session);
    }

    /** Deletes the one row whose id column(s) match {@code paramMap}'s values for {@code info.getIds()}. */
    @Transactional
    public void delete(EntityInfo info, Map<String, Object> paramMap) {
        String where = String.join(" AND ", info.getIds().stream().map(c -> c + " = #{" + c + "}").toList());
        runSql("DELETE FROM " + info.getTableName() + " WHERE " + where, paramMap);
    }
}
