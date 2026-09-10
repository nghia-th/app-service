package vn.org.thn.app.base.i18n.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/** Request body for adding/updating one translation key across languages. Ported from the Kotlin original's {@code LanguageRequest}. */
@Data
public class LanguageRequest {

    @Schema(type = "string", example = "hello", description = "key language")
    private String langKey = "";

    @Schema(type = "object", example = "{\"vi\":\"hello\",\"en\":\"hello\"}", description = "data")
    private Map<String, String> mapValues = Map.of();
}
