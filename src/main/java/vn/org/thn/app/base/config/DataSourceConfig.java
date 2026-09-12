package vn.org.thn.app.base.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.app.base.persistence.datasource.DatabaseProvider;
import vn.org.thn.app.base.persistence.migration.DatabaseInitializer;

import javax.sql.DataSource;
import java.util.List;

/**
 * Builds the single application {@link DataSource} (a HikariCP pool) from whichever
 * {@link DatabaseProvider} matches {@link DatabaseProperties#getType()}: driver class name and
 * JDBC URL come from that provider, username/password from Spring Boot's own
 * {@link DataSourceProperties}, and pool sizing/timeouts from {@link DatabaseProperties.Pool}. Before the
 * pool itself is built, {@link DatabaseProvider#ensureDatabaseExists} runs so a target database
 * that doesn't exist yet gets created instead of failing the pool's very first connection.
 */
@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, DatabaseProperties.class})
public class DataSourceConfig {

    /** Resolves the active engine's provider, then configures and starts a Hikari connection pool for it. */
    @Bean
    public DataSource dataSource(DataSourceProperties properties, List<DatabaseProvider> providers,
                                  DatabaseProperties databaseProperties) {
        DatabaseProvider provider = DatabaseInitializer.resolveProvider(providers, databaseProperties.getType());
        provider.ensureDatabaseExists(properties);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(provider.driverClassName());
        config.setJdbcUrl(provider.jdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setMaximumPoolSize(databaseProperties.getPool().getMaxPoolSize());
        config.setMinimumIdle(databaseProperties.getPool().getMinIdle());
        config.setConnectionTimeout(databaseProperties.getPool().getConnectionTimeoutMs());
        config.setMaxLifetime(databaseProperties.getPool().getMaxLifetimeMs());
        config.setLeakDetectionThreshold(databaseProperties.getPool().getLeakDetectionThresholdMs());
        return new HikariDataSource(config);
    }
}
