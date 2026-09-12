package vn.org.thn.app.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void testToUnaccentWithVietnamese() {
        assertEquals("truong hieu nghia", StringUtils.toUnaccent("Trương Hiếu Nghĩa"));
        assertEquals("da nang - duong di", StringUtils.toUnaccent("Đà Nẵng - Đường Đi"));
        assertEquals("cong hoa xa hoi chu nghia viet nam", StringUtils.toUnaccent("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM"));
        assertEquals("nguyen van a", StringUtils.toUnaccent("   Nguyễn   Văn   A   "));
    }

    @Test
    void testToUnaccentEdgeCases() {
        assertNull(StringUtils.toUnaccent(null));
        assertEquals("", StringUtils.toUnaccent(""));
        assertEquals("", StringUtils.toUnaccent("    "));
        assertEquals("hello world", StringUtils.toUnaccent("hello world"));
    }
}
