package vn.org.thn.app.base.i18n.domain;

import vn.org.thn.app.base.i18n.config.LanguageAutoConfiguration;
import vn.org.thn.app.base.i18n.service.LanguageService;
import vn.org.thn.app.base.i18n.api.LanguageApi;
import vn.org.thn.app.base.i18n.api.LanguageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory translation store: {@code get(lang).get(langKey) -> value}. One singleton per
 * application (see {@link LanguageAutoConfiguration#language}), loaded from the "lang/*.json"
 * files at startup and mutated in place by {@link LanguageService} on every add/update/delete -
 * a lookup never re-reads disk. Ported from the Kotlin original's {@code Language}.
 * <p>
 * The outer map and every per-language inner map are backed by {@link ConcurrentHashMap}, not
 * plain {@code HashMap}: this bean is a singleton read and written concurrently by many HTTP
 * request threads (every call through {@link LanguageApi}), and a plain {@code HashMap} is not
 * safe under concurrent writes - it can corrupt its internal structure or throw
 * {@code ConcurrentModificationException} under real traffic, not just in theory. Purely local,
 * single-call working maps elsewhere in this class (e.g. inside {@link #loadList}) stay plain
 * {@code HashMap} since they never escape one method call.
 */
public class Language {

    private final Map<String, Map<String, String>> values;

    public Language() {
        this(new ConcurrentHashMap<>());
    }

    /** Defensively copies the given {@code lang -> (langKey -> value)} data into concurrent-safe maps. */
    public Language(Map<String, Map<String, String>> initialValues) {
        Map<String, Map<String, String>> copy = new ConcurrentHashMap<>();
        initialValues.forEach((lang, inner) -> copy.put(lang, new ConcurrentHashMap<>(inner)));
        this.values = copy;
    }

    public Map<String, Map<String, String>> getValues() {
        return values;
    }

    /** One translated value; falls back to "en" when {@code lang} is null, and to {@code key} itself when missing. */
    public String get(String lang, String key) {
        Map<String, String> langMap = values.get(lang != null ? lang : "en");
        return langMap != null ? langMap.getOrDefault(key, key) : key;
    }

    /** Applies one key's per-language values from the request onto the in-memory store (no file/DB write here - see {@link LanguageService}). */
    public void updateLanguage(LanguageRequest request) {
        request.getMapValues().forEach((lang, value) -> {
            Map<String, String> langMap = values.computeIfAbsent(lang, k -> new ConcurrentHashMap<>());
            langMap.put(request.getLangKey(), value == null ? "" : value.trim());
        });
    }

    /** Removes one key from every language's map. */
    public void delete(String langKey) {
        values.values().forEach(map -> map.remove(langKey));
    }

    /** Rows shaped for an admin UI: one row per langKey, one column per language. */
    public List<Map<String, String>> loadList(String keyword) {
        Map<String, Map<String, String>> filtered = new HashMap<>();

        if (keyword == null || keyword.isBlank()) {
            filtered.putAll(values);
        } else {
            Set<String> matchedKeys = findInnerKeysByKeyword(values, keyword);
            values.forEach((lang, map) -> {
                Map<String, String> inner = filtered.computeIfAbsent(lang, k -> new HashMap<>());
                matchedKeys.forEach(key -> inner.put(key, map.getOrDefault(key, "")));
            });
        }

        Set<String> langKeys = new LinkedHashSet<>();
        filtered.values().forEach(map -> langKeys.addAll(map.keySet()));

        List<Map<String, String>> result = new ArrayList<>();
        for (String langKey : langKeys) {
            Map<String, String> row = new HashMap<>();
            row.put("langKey", langKey);
            for (String lang : filtered.keySet()) {
                row.put(lang, filtered.get(lang).getOrDefault(langKey, ""));
            }
            result.add(row);
        }
        return result;
    }

    /** Keys (of any language) whose key name or value contains {@code keyword}, case-insensitively. */
    private static Set<String> findInnerKeysByKeyword(Map<String, Map<String, String>> data, String keyword) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        Set<String> matched = new LinkedHashSet<>();
        for (Map<String, String> innerMap : data.values()) {
            innerMap.forEach((innerKey, innerValue) -> {
                boolean keyMatches = innerKey.toLowerCase(Locale.ROOT).contains(lowerKeyword);
                boolean valueMatches = innerValue != null && innerValue.toLowerCase(Locale.ROOT).contains(lowerKeyword);
                if (keyMatches || valueMatches) {
                    matched.add(innerKey);
                }
            });
        }
        return matched;
    }
}
