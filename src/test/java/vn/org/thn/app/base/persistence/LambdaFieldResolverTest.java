package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.persistence.lambda.LambdaFieldResolver;

import static org.junit.jupiter.api.Assertions.*;

class LambdaFieldResolverTest {

    @Test
    @DisplayName("Should resolve getter method reference to entity field name")
    void resolve_getterMethodReference_returnsFieldName() {
        String langKeyField = LambdaFieldResolver.resolve(Translate::getLangKey);
        String langField = LambdaFieldResolver.resolve(Translate::getLang);
        String valueField = LambdaFieldResolver.resolve(Translate::getValue);

        assertEquals("langKey", langKeyField);
        assertEquals("lang", langField);
        assertEquals("value", valueField);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when method reference is null")
    void resolve_nullReference_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> LambdaFieldResolver.resolve(null));
    }
}
