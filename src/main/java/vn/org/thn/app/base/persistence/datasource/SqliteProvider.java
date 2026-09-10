package vn.org.thn.app.base.persistence.datasource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** {@link DatabaseProvider} for SQLite - the zero-config default engine (a single local file, no server to run). */
@Component
public class SqliteProvider implements DatabaseProvider {

    @Value("${base.database.sqlite.file-name:app.db}")
    private String fileName;

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLITE;
    }

    @Override
    public String driverClassName() {
        return "org.sqlite.JDBC";
    }

    @Override
    public String migrationFolder() {
        return "sqlite";
    }

    /** Builds the file-based JDBC URL under {@link DatabasePath#DATA_DIR}, creating that directory first if needed. */
    @Override
    public String jdbcUrl() {
        try {
            Files.createDirectories(DatabasePath.DATA_DIR);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data directory: " + DatabasePath.DATA_DIR, e);
        }
        return "jdbc:sqlite:" + DatabasePath.DATA_DIR.resolve(fileName).toAbsolutePath();
    }

    /** Sets WAL journaling, NORMAL synchronous mode, and enables foreign-key enforcement - SQLite defaults to all three off/legacy. */
    @Override
    public void initialize(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize SQLite pragmas", e);
        }
    }
}
