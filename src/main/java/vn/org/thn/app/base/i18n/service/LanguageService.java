package vn.org.thn.app.base.i18n.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import vn.org.thn.app.base.IBase;
import vn.org.thn.app.base.i18n.config.LanguageAutoConfiguration;
import vn.org.thn.app.base.i18n.api.LanguageRequest;
import vn.org.thn.app.base.i18n.domain.Language;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.i18n.repository.TranslateRepository;
import vn.org.thn.app.base.util.JsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes every {@link Language} change through to disk (and to {@link Translate} via
 * {@link TranslateRepository}, for a persistent DB copy). Ported from the Kotlin original's
 * {@code LanguageRepo} - renamed, since this doesn't extend {@code BaseRepositoryImpl} or touch
 * SQL directly, "Service" fits the Java/Spring convention better than "Repo". Also a proper
 * {@code @Service} bean now (constructor/field {@code @Autowired}) instead of the original's
 * manually-{@code new}'d class that reached its dependencies through the
 * {@code ApplicationContextProvider} service-locator - see {@link vn.org.thn.app.base.IBase}
 * for why that pattern was dropped project-wide.
 * <p>
 * Fixed one bug versus the original: {@code deleteLanguage} built a {@code DeleteBuilder}
 * condition on {@code translate} but never called {@code execute()}, so the DB row was never
 * actually removed - only the in-memory store and the JSON files were updated.
 * <p>
 * {@code @DependsOn("flyway")}: {@link #loadLanguage()} queries {@link Translate} via
 * {@link TranslateRepository} from a {@code @PostConstruct} method, so it needs the
 * {@code translate} table to already exist. {@code FlywayConfig}'s {@code flyway} bean is a plain
 * {@code @Bean(initMethod = "migrate")} (not Spring Boot's auto-configured Flyway integration), so
 * there is no automatic {@code @DependsOn} wiring between it and any MyBatis
 * repository/{@code SqlSessionFactory} bean sharing the same {@code DataSource} - without this
 * annotation, Spring is free to build this bean (and run its {@code @PostConstruct}) before
 * {@code flyway} has migrated anything, which is exactly what happened switching to SQLite (single
 * pooled connection, fresh file) - {@code translateRepository.query().list()} failed with
 * "no such table: translate" because migrations simply hadn't run yet.
 */
@Service
@DependsOn("flyway")
public class LanguageService extends IBase {

    private static final String REACT_LANG_PATH = "html" + File.separator + "%s" + File.separator + "languages";

    @Autowired
    private Language language;

    @Autowired
    private TranslateRepository translateRepository;

    /** Where the React build served by this service keeps its own copy of the language JSON files. */
    @Value("${lang.react-build:build}")
    private String reactBuild = "build";


    /**
     * Runs once at application startup: merges every {@link Translate} row already persisted in
     * the database on top of the file-based {@link Language} store {@link LanguageAutoConfiguration}
     * just built, writes every key of the merged result (file-only keys included, not just the ones
     * that came from the database) back to the {@code translate} table, then rewrites lang/*.json +
     * the React build's copy so both reflect the merge.
     * <p>
     * The DB-merge step keeps a translation an admin edited earlier (durable only in the
     * {@code translate} table) in effect even after a fresh deploy resets {@code lang/*.json} back
     * to its shipped defaults - without it, that edit would silently revert to the shipped value.
     * <p>
     * A DB row whose {@code lang} isn't one of the languages already loaded into {@code language}
     * gets a brand new entry created for it (unlike a missing {@code langKey}, which just adds one
     * more key to an existing language) - DB data is allowed to grow the set of languages the app
     * serves beyond {@code lang.support}/whatever files happened to already be on disk.
     * <p>
     * The write-back step re-saves every key on every startup, not only ones actually changed by
     * this merge - {@code save} is an upsert keyed on {@code (langKey, lang)} so this is idempotent,
     * just redundant I/O against rows that already match; it exists so a key that only ever lived in
     * a shipped JSON file (never edited through the admin UI) still ends up backed by a DB row too,
     * same as the Kotlin original did.
     */
    @PostConstruct
    public void loadLanguage() {
        Set<String> existingDbKeys = new HashSet<>();
        translateRepository.query().list().forEach(row -> {
            existingDbKeys.add(row.getLang() + "::" + row.getLangKey());
            language.getValues()
                    .computeIfAbsent(row.getLang(), k -> new ConcurrentHashMap<>())
                    .put(row.getLangKey(), row.getValue());
        });

        List<Translate> newTranslates = new ArrayList<>();
        language.getValues().forEach((lang, values) -> values.forEach((langKey, value) -> {
            if (!existingDbKeys.contains(lang + "::" + langKey)) {
                Translate translate = new Translate();
                translate.setLangKey(langKey);
                translate.setLang(lang);
                translate.setValue(value == null ? "" : value);
                newTranslates.add(translate);
            }
        }));

        if (!newTranslates.isEmpty()) {
            translateRepository.saveAll(newTranslates);
        }

        writeFileData();
    }

    /**
     * Applies {@code request} to the in-memory {@link Language} store, rewrites every language's
     * JSON file to disk (both the {@code lang/} copy and the React build's copy), and persists one
     * {@link Translate} row per language in {@code request}'s value map.
     */
    public void updateLanguage(LanguageRequest request) {
        language.updateLanguage(request);
        writeFileData();
        request.getMapValues().forEach((lang, value) -> {
            Translate translate = new Translate();
            translate.setLangKey(request.getLangKey());
            translate.setLang(lang);
            translate.setValue(value == null ? "" : value.trim());
            translateRepository.save(translate);
        });
    }

    /** Removes {@code langKey} from the in-memory store, rewrites every language's JSON file to disk, and deletes its {@link Translate} row(s) from the database. */
    public void deleteLanguage(String langKey) {
        language.delete(langKey);
        writeFileData();
        translateRepository.delete().eq(Translate::getLangKey, langKey).execute();
    }

    /**
     * Rewrites, for every language in the in-memory store, both its {@code lang/<lang>.json} file
     * and the React build's copy under {@code html/<reactBuild>/languages/<lang>.json} - both
     * shaped {@code {"data": {...}}}, matching what {@link LanguageAutoConfiguration#language()}
     * expects to read back on the next startup.
     */
    private synchronized void writeFileData() {
        String currentPath = LanguageAutoConfiguration.currentPath();
        language.getValues().forEach((lang, values) -> {
            JsonUtils.toJsonFile(new File(currentPath + "lang" + File.separator + lang + ".json"), Map.of("data", values));

            String reactFilePath = currentPath + String.format(REACT_LANG_PATH, reactBuild) + File.separator + lang + ".json";
            JsonUtils.toJsonFile(new File(reactFilePath), Map.of("data", values));
        });
    }
}
