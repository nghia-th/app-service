package vn.org.thn.app.base.i18n.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.core.type.TypeReference;
import vn.org.thn.app.base.i18n.domain.Language;
import vn.org.thn.app.base.i18n.service.LanguageService;
import vn.org.thn.app.base.util.JsonUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Auto-registers the {@link Language} singleton, loaded once at startup from the "lang/*.json"
 * files under the working directory (creating any missing file for a supported language, same as
 * the Kotlin original's {@code BaseConfiguration.language()} bean). {@link LanguageService} keeps
 * this same instance and the files in sync on every write afterwards.
 */
@AutoConfiguration
public class LanguageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LanguageAutoConfiguration.class);

    /** Same property key as the Kotlin original, comma-separated language codes, e.g. "vi,en". */
    @Value("${lang.support:vi,en}")
    private Set<String> langSupport;

    /**
     * Builds the {@link Language} singleton at startup: ensures the {@code lang/} directory and one
     * JSON file per supported language exist (creating empty ones as needed), then loads every
     * {@code lang/*.json} file's content (not just the supported-languages list, so a manually added
     * extra language file is picked up too) into the in-memory store. Falls back to an empty
     * {@link Language} instance, logging the error, rather than failing application startup if
     * anything goes wrong reading the directory.
     * <p>
     * Each file is shaped {@code {"data": {"key": "value", ...}}} - the same wrapper
     * {@link LanguageService#writeFileData()} writes and the one the React build's own copy under
     * {@code html/<reactBuild>/languages/<lang>.json} already used, e.g. the real
     * {@code quiz-service/lang/vi.json} on disk. Not a flat {@code {"key": "value"}} object.
     */
    @Bean
    @ConditionalOnMissingBean
    public Language language() {
        try {
            File langDir = new File(currentPath() + "lang");
            if (!langDir.exists()) {
                langDir.mkdirs();
            }

            Map<String, Map<String, String>> result = new HashMap<>();
            for (String lang : langSupport) {
                File file = new File(langDir, lang + ".json");
                if (!file.exists()) {
                    JsonUtils.toJsonFile(file, Map.of("data", Map.of()));
                }
                result.put(lang, new HashMap<>());
            }

            File[] files = langDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isFile()) {
                        continue;
                    }
                    String lang = stripExtension(file.getName());
                    Map<String, Map<String, String>> wrapper = JsonUtils.toObject(
                            Files.readString(file.toPath()), new TypeReference<Map<String, Map<String, String>>>() {
                            });
                    Map<String, String> content = wrapper != null ? wrapper.get("data") : null;
                    result.put(lang, content != null ? new HashMap<>(content) : new HashMap<>());
                }
            }
            return new Language(result);
        } catch (Exception e) {
            log.error("Cannot load lang/*.json - starting with an empty Language store", e);
            return new Language();
        }
    }

    /** Working directory, always ending with a path separator - matches the Kotlin original's {@code currentPath()}. */
    public static String currentPath() {
        String path = System.getProperty("user.dir");
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /** Removes a trailing {@code .ext} from a file name, e.g. {@code "vi.json"} -> {@code "vi"}; returns {@code fileName} unchanged if it has no extension. */
    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
