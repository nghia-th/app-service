package vn.org.thn.app.base.persistence.executor;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;

/**
 * Hands out a plain MyBatis mapper proxy for a service that needs a hand-written mapper
 * interface + its own XML/annotated statements, alongside the generic DSL. (The Kotlin original
 * used a reified inline function for {@code mapper<M>()}; Java has no reified generics, so callers
 * pass the {@code Class<M>} directly - see {@code BaseRepositoryImpl#mapper(Class)}.)
 */
@Component
public class MapperExecutor {

    private final SqlSessionTemplate session;

    public MapperExecutor(SqlSessionTemplate session) {
        this.session = session;
    }

    /** Standard MyBatis {@code SqlSession#getMapper} proxy for a hand-written mapper interface. */
    public <T> T mapper(Class<T> clazz) {
        return session.getMapper(clazz);
    }
}
