package vn.org.thn.app.base.i18n.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.i18n.domain.TranslateId;
import vn.org.thn.app.base.persistence.repository.BaseRepositoryImpl;

/**
 * Repository for {@link Translate}, ready to use as-is by any service depending on {@code base}
 * (picked up by component scan the same way as the module's other {@code @Repository}/
 * {@code @Component} beans - see {@code BaseRepositoryImpl}'s executors). Ported from the Kotlin
 * original's {@code TranslateRepo}; the {@code clazz()} override it needed for logging is gone
 * since {@code IBase}'s logger no longer needs it (resolved from {@code getClass()} instead).
 */
@Repository
public class TranslateRepository extends BaseRepositoryImpl<Translate, TranslateId> {
}
