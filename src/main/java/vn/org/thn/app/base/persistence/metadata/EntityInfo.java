package vn.org.thn.app.base.persistence.metadata;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Reflection-derived table/column metadata for one @Entity class. Cached in {@link EntityCache}. */
public class EntityInfo {

    private String tableName;
    private final Map<String, Field> fieldMap = new LinkedHashMap<>();
    private final Map<String, String> columns = new LinkedHashMap<>(); // column -> field name
    private final Map<String, String> fieldColumns = new LinkedHashMap<>(); // field name -> column (reverse of columns)
    private final Set<String> ids = new LinkedHashSet<>();             // column names that are (part of) the PK
    private String identityColumn;                                    // column name with @GeneratedValue(IDENTITY), if any
    private boolean autoIdentity;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, Field> getFieldMap() {
        return fieldMap;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    /** field name -> column name, used by the query DSL to turn a resolved field (from a string or a
     *  method reference) into the actual SQL column, honoring any @Column(name=...) override. */
    public Map<String, String> getFieldColumns() {
        return fieldColumns;
    }

    public Set<String> getIds() {
        return ids;
    }

    public String getIdentityColumn() {
        return identityColumn;
    }

    public void setIdentityColumn(String identityColumn) {
        this.identityColumn = identityColumn;
    }

    public boolean isAutoIdentity() {
        return autoIdentity;
    }

    public void setAutoIdentity(boolean autoIdentity) {
        this.autoIdentity = autoIdentity;
    }
}
