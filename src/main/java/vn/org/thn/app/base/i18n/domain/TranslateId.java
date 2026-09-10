package vn.org.thn.app.base.i18n.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Composite id for {@link Translate}: (langKey, lang). Field names must match {@link Translate}'s
 * exactly - {@code BaseRepositoryImpl#withCompositeId} reads them by field name via reflection to
 * build the WHERE clause, the same mechanism any other composite-key entity uses.
 * <p>
 * {@code @Data} does not generate a constructor here since two explicit constructors already
 * exist on this class - Lombok's {@code @RequiredArgsConstructor} (part of {@code @Data}) is
 * skipped whenever any constructor is already declared.
 */
@Data
public class TranslateId implements Serializable {

    private String langKey;
    private String lang;

    public TranslateId() {
    }

    public TranslateId(String langKey, String lang) {
        this.langKey = langKey;
        this.lang = lang;
    }
}
