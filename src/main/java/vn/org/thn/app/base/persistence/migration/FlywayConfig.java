package vn.org.thn.app.base.persistence.migration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.app.base.config.DatabaseProperties;
import vn.org.thn.app.base.persistence.datasource.DatabasePath;
import vn.org.thn.app.base.persistence.datasource.DatabaseType;


import javax.sql.DataSource;
import java.nio.file.Path;

/**
 * Runs Flyway migrations from the folder matching the active engine ({@code database/<type>/}, see
 * {@link DatabasePath#migrationFolder}) against the application {@link DataSource}, once at startup.
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class FlywayConfig {

    /** {@code initMethod = "migrate"} runs the migration as soon as this bean is created - no separate call needed anywhere. */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource, DatabaseProperties databaseProperties) {
        Path folder = DatabasePath.migrationFolder(databaseProperties.getType()).toAbsolutePath();
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("filesystem:" + folder)
                // SQLite has no ALTER COLUMN / DROP NOT NULL / DROP COLUMN etc., so changing a
                // column there requires the standard "rebuild the table" dance: PRAGMA
                // foreign_keys=OFF, CREATE the new table, copy the data across, DROP the old one,
                // RENAME the new one back, PRAGMA foreign_keys=ON (see e.g.
                // database/sqlite/V18__question_answer_mode_nullable.sql). Flyway treats a PRAGMA
                // as non-transactional and, by default (mixed=false), refuses to run a migration
                // that mixes non-transactional and transactional statements in one script - this
                // pattern will keep recurring for future SQLite column changes. Scoped to SQLite
                // only (not a blanket `.mixed(true)` for every engine) so a failed migration on
                // Postgres/MySQL/SQL Server/Oracle still rolls back cleanly instead of possibly
                // leaving the database half-migrated - see Medium finding #12 (2026-09-12 review).
                .mixed(databaseProperties.getType() == DatabaseType.SQLITE)
                .load();
    }
}
