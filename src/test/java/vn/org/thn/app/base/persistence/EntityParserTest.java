package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.persistence.metadata.EntityParser;

import static org.junit.jupiter.api.Assertions.*;

class EntityParserTest {

    @Test
    @DisplayName("Should parse @Entity metadata correctly")
    void parse_validEntity_returnsEntityInfo() {
        EntityInfo info = EntityParser.parse(Translate.class);

        assertNotNull(info);
        assertEquals("translate", info.getTableName());
        assertTrue(info.getIds().contains("lang_key"));
        assertTrue(info.getIds().contains("lang"));
        assertEquals("langKey", info.getColumns().get("lang_key"));
        assertEquals("lang", info.getColumns().get("lang"));
        assertEquals("value", info.getColumns().get("value"));
    }

    @Test
    @DisplayName("Should return null when class has no @Entity annotation")
    void parse_nonEntity_returnsNull() {
        EntityInfo info = EntityParser.parse(String.class);
        assertNull(info);
    }
}
