package vn.org.thn.app.base.i18n.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.response.ApiResponse;
import vn.org.thn.app.base.i18n.config.LanguageAutoConfiguration;
import vn.org.thn.app.base.i18n.domain.Language;
import vn.org.thn.app.base.i18n.repository.TranslateRepository;
import vn.org.thn.app.base.i18n.service.LanguageService;
import vn.org.thn.app.base.web.controller.BaseCtl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Translation management API, ready to use as-is by any service depending on {@code base}
 * (picked up by component scan, same as {@link TranslateRepository}). Ported from the Kotlin
 * original's {@code LanguageApi} - dependencies are {@code @Autowired} fields instead of the
 * original's {@code autoWired()}/manually-{@code new}'d {@code LanguageRepo()}, and responses go
 * through {@link BaseCtl}'s real {@link ApiResponse} instead of a free-form int status code.
 * <p>
 * {@link #addOrUpdate} validates every language code in the request against {@link #LANG_CODE_PATTERN}
 * before it ever reaches {@link LanguageService}: a language code eventually becomes part of a
 * file name ({@code lang/<code>.json}) in {@link LanguageService#updateLanguage}, and this endpoint
 * is unauthenticated (no security/JWT layer in {@code base} - see {@link vn.org.thn.service.base.IBase}),
 * so an unvalidated code would be a path-traversal opening (e.g. a code like {@code ../../evil}).
 */
@RestController
@RequestMapping("/public/language")
public class LanguageApi extends BaseCtl {

    /** Conservative charset for a language/locale code (e.g. "vi", "en", "zh-CN") - blocks path separators and traversal sequences. */
    private static final Pattern LANG_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,20}$");

    @Autowired
    private Language language;

    @Autowired
    private LanguageService languageService;

    /** All translation rows, one per langKey, optionally filtered to those whose key or value contains {@code keyword}. */
    @Operation(
            summary = "List language translations",
            description = "Get list of all translation keys with values for each language, optionally filtered by keyword"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved translation list")
    })
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> list(
            @RequestParam(required = false) String keyword) {
        return ok(language.loadList(keyword));
    }

    /** Adds a new translation key or updates an existing one's per-language values. */
    @Operation(
            summary = "Add or update translation key",
            description = "Add a new translation key or update existing values for specified language codes"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully saved translation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed (blank langKey or invalid language code)")
    })
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<Void>> addOrUpdate(@RequestBody LanguageRequest request) {
        if (request.getLangKey() == null || request.getLangKey().isBlank()) {
            return fail(CommonErrorCode.VALIDATION_FAILED, "langKey must not be blank");
        }
        for (String lang : request.getMapValues().keySet()) {
            if (!LANG_CODE_PATTERN.matcher(lang).matches()) {
                return fail(CommonErrorCode.VALIDATION_FAILED, "invalid language code: " + lang);
            }
        }
        languageService.updateLanguage(request);
        return ok();
    }

    /** Deletes one translation key (across every language). Body is the raw langKey as a JSON string. */
    @Operation(
            summary = "Delete translation key",
            description = "Delete one translation key across all languages"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully deleted translation key")
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody String langKey) {
        languageService.deleteLanguage(langKey);
        return ok();
    }

    /** Deletes several translation keys at once. Body is a JSON array of langKeys. */
    @Operation(
            summary = "Delete multiple translation keys",
            description = "Delete a list of translation keys across all languages"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully deleted translation keys")
    })
    @DeleteMapping("/deletes")
    public ResponseEntity<ApiResponse<Void>> deletes(@RequestBody List<String> langKeys) {
        langKeys.forEach(languageService::deleteLanguage);
        return ok();
    }

    /** All translations for one language, keyed by langKey. */
    @Operation(
            summary = "Get translations for a single language",
            description = "Get map of key-value translations for a specific language code (e.g., 'vi', 'en')"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved language translations")
    })
    @GetMapping("/{lang}")
    public ResponseEntity<ApiResponse<Map<String, String>>> lang(@PathVariable("lang") String lang) {
        return ok(language.getValues().get(lang));
    }

    /** Every language's translations at once, keyed by language code then langKey. */
    @Operation(
            summary = "Get all language translations",
            description = "Get nested map of all language codes to their key-value translations"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved all translations")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Map<String, String>>>> langAll() {
        return ok(language.getValues());
    }

    /** Streams every "lang/*.json" file as a single "lang.zip" download; 404 if the lang/ folder doesn't exist. */
    @Operation(
            summary = "Export translation JSON files as ZIP",
            description = "Download a zip archive containing all JSON translation files from the lang directory"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully generated zip archive"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Language directory not found")
    })
    @PostMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        Path langDir = Path.of(LanguageAutoConfiguration.currentPath() + "lang");
        if (!Files.exists(langDir) || !Files.isDirectory(langDir)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"lang.zip\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
             var files = Files.walk(langDir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    zipOut.putNextEntry(new ZipEntry(langDir.relativize(file).toString()));
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
