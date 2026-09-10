package vn.org.thn.app.base.persistence.migration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.config.DatabaseProperties;
import vn.org.thn.app.base.persistence.datasource.DatabaseProvider;
import vn.org.thn.app.base.persistence.datasource.DatabaseType;

import javax.sql.DataSource;
import java.util.List;

/** Runs the DatabaseProvider matching the configured DatabaseType's one-time init (e.g. SQLite PRAGMAs). */
@Component
public class DatabaseInitializer {

    private final List<DatabaseProvider> providers;
    private final DatabaseProperties properties;
    private final DataSource dataSource;

    public DatabaseInitializer(List<DatabaseProvider> providers, DatabaseProperties properties, DataSource dataSource) {
        this.providers = providers;
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /** Runs once after the bean and its dependencies (including the already-built {@link DataSource}) are ready. */
    @PostConstruct
    public void init() {
        DatabaseProvider provider = resolveProvider(providers, properties.getType());
        provider.initialize(dataSource);
    }

    /**
     * Finds the {@link DatabaseProvider} matching {@code type} among all discovered provider beans.
     * Also used by {@link vn.org.thn.service.base.db.config.DataSourceConfig} to resolve the
     * driver/JDBC URL before the {@link DataSource} bean even exists.
     *
     * @throws IllegalStateException if no provider is registered for {@code type} (e.g. the
     *                                configured engine's provider class was never added as a bean)
     */
    public static DatabaseProvider resolveProvider(List<DatabaseProvider> providers, DatabaseType type) {
        return providers.stream()
                .filter(p -> p.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No DatabaseProvider registered for type " + type
                                + " (available: " + providers.stream().map(p -> p.type().name()).toList() + ")"));
    }
}
