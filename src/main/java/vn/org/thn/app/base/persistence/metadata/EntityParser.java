package vn.org.thn.app.base.persistence.metadata;

import org.apache.commons.lang3.reflect.FieldUtils;
import vn.org.thn.app.base.persistence.annotation.*;
import vn.org.thn.app.base.persistence.annotation.*;
import vn.org.thn.app.base.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/** Reads @Entity/@Table/@Column/@Id/@GeneratedValue/@Transient off a class into an {@link EntityInfo}. */
public final class EntityParser {

    private EntityParser() {
    }

    /**
     * Parses {@code clazz}'s table/column/id metadata by reflection. Returns null if {@code clazz}
     * has no {@code @Entity} annotation (the caller, {@link EntityCache#get}, turns that into an
     * {@link IllegalArgumentException}). Skips {@code @Transient} and {@code static} fields; a
     * field's column name defaults to its snake_cased Java name, overridable via
     * {@code @Column(name=...)}; a field carrying {@code @Id} is added to the id set, and if it
     * also carries {@code @GeneratedValue(strategy = IDENTITY)}, it becomes the identity column.
     */
    public static EntityInfo parse(Class<?> clazz) {
        if (clazz.getAnnotation(Entity.class) == null) {
            return null;
        }

        EntityInfo info = new EntityInfo();

        Table table = clazz.getAnnotation(Table.class);
        String tableName = (table != null && !table.name().isBlank()) ? table.name() : clazz.getSimpleName();
        info.setTableName(tableName);

        List<Field> fields = FieldUtils.getAllFieldsList(clazz);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);

            String column = StringUtils.camelToSnake(field.getName());
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && !columnAnnotation.name().isBlank()) {
                column = columnAnnotation.name();
            }

            info.getColumns().put(column, field.getName());
            info.getFieldMap().put(column, field);
            info.getFieldColumns().put(field.getName(), column);

            if (field.isAnnotationPresent(Id.class)) {
                info.getIds().add(column);
                GeneratedValue generated = field.getAnnotation(GeneratedValue.class);
                if (generated != null && generated.strategy() == GenerationType.IDENTITY) {
                    info.setIdentityColumn(column);
                    info.setAutoIdentity(true);
                }
            }
        }

        return info;
    }
}
