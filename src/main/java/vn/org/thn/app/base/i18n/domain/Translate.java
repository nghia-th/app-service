package vn.org.thn.app.base.i18n.domain;

import lombok.Data;
import vn.org.thn.app.base.i18n.service.LanguageService;
import vn.org.thn.app.base.i18n.api.LanguageApi;
import vn.org.thn.app.base.persistence.annotation.Entity;
import vn.org.thn.app.base.persistence.annotation.Id;
import vn.org.thn.app.base.persistence.annotation.Table;

import java.io.Serializable;

/**
 * Key/value translation row: one row per (langKey, lang) pair, e.g. ("welcome.message", "vi").
 * Ported from the Kotlin original's {@code Translate}. This is the DB-backed copy written
 * alongside the file-based store every time {@link LanguageService} updates a translation -
 * see {@link Language}/{@link LanguageApi} for the live in-memory store + REST API a frontend
 * actually reads from.
 */
@Data
@Entity
@Table(name = "translate")
public class Translate implements Serializable {

    @Id
    private String langKey = "";

    @Id
    private String lang = "vi";

    private String value = "";
}
