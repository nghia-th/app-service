package vn.org.thn.app.base.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    static class SampleData {
        private String name;
        private int age;

        public SampleData() {}

        public SampleData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @Test
    @DisplayName("Should serialize object to JSON string")
    void toJson_validObject_returnsJsonString() {
        SampleData data = new SampleData("Antigravity", 3);
        String json = JsonUtils.toJson(data);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"Antigravity\""));
        assertTrue(json.contains("\"age\":3"));
    }

    @Test
    @DisplayName("Should deserialize JSON string to object")
    void toObject_validJson_returnsObject() {
        String json = "{\"name\":\"Alice\",\"age\":25}";
        SampleData data = JsonUtils.toObject(json, SampleData.class);

        assertNotNull(data);
        assertEquals("Alice", data.getName());
        assertEquals(25, data.getAge());
    }

    @Test
    @DisplayName("Should deserialize JSON array string to List")
    void toList_validJsonArray_returnsList() {
        String json = "[{\"name\":\"Bob\",\"age\":30},{\"name\":\"Charlie\",\"age\":35}]";
        List<SampleData> list = JsonUtils.toList(json, SampleData.class);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("Bob", list.get(0).getName());
        assertEquals("Charlie", list.get(1).getName());
    }

    @Test
    @DisplayName("Should convert object to Map")
    void toMap_validObject_returnsMap() {
        SampleData data = new SampleData("Dave", 40);
        Map<String, Object> map = JsonUtils.toMap(data);

        assertNotNull(map);
        assertEquals("Dave", map.get("name"));
        assertEquals(40, map.get("age"));
    }
}
