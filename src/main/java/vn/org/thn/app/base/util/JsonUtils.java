package vn.org.thn.app.base.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Common JSON <-> Object / List / Map conversions, backed by a single shared
 * Jackson 3 {@link JsonMapper} (java.time support such as LocalDateTime is
 * built into jackson-databind 3.x, no separate module needed).
 *
 * This mapper is independent from the JsonMapper bean Spring MVC uses for
 * @RequestBody/@ResponseBody -- use this class for ad-hoc conversions (caching,
 * logging, mapping between DTO/Map/Entity, calling external APIs), not to
 * change how controllers serialize responses.
 */
public final class JsonUtils {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private JsonUtils() {
    }

    /** Object (bean, List, Map, ...) -> JSON string. */
    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot convert object to JSON: " + e.getMessage(), e);
        }
    }

    /** Object -> pretty-printed JSON string, for logging/debugging. */
    public static String toJsonPretty(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot convert object to pretty JSON: " + e.getMessage(), e);
        }
    }

    /** JSON string -> object of a simple (non-generic) type, e.g. toObject(json, UserDTO.class). */
    public static <T> T toObject(String json, Class<T> targetType) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, targetType);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot convert JSON to " + targetType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * JSON string -> object of a generic type, e.g.
     * toObject(json, new TypeReference&lt;PageResponse&lt;UserDTO&gt;&gt;() {}).
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot convert JSON to " + typeReference.getType() + ": " + e.getMessage(), e);
        }
    }

    /** JSON array string -> List<T>, e.g. toList(json, UserDTO.class). */
    public static <T> List<T> toList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            var listType = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return MAPPER.readValue(json, listType);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot convert JSON to List<" + elementType.getSimpleName() + ">: " + e.getMessage(), e);
        }
    }

    /** JSON object string -> Map<String, Object> (values kept as Object: nested Map/List/String/Number). */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return toObject(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /** Bean/List/Map -> Map<String, Object>, without an intermediate JSON string (e.g. DTO -> Map for a MyBatis param map). */
    public static Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        return MAPPER.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Object -> pretty-printed JSON file (creates parent directories if missing). For writing
     * small config/data files (e.g. the "lang/*.json" translation files), not for large payloads.
     */
    public static void toJsonFile(File file, Object value) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new JsonConvertException("Cannot create directory: " + parent, null);
        }
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, value);
        } catch (JacksonException e) {
            throw new JsonConvertException("Cannot write JSON to file " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Direct object -> object conversion (no JSON string round-trip), e.g.
     * convert(userMap, UserDTO.class) or convert(userEntity, UserDTO.class).
     * Fields are matched by name, same as JSON (de)serialization rules.
     */
    public static <T> T convert(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(source, targetType);
        } catch (IllegalArgumentException | JacksonException e) {
            throw new JsonConvertException("Cannot convert " + source.getClass().getSimpleName()
                    + " to " + targetType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
