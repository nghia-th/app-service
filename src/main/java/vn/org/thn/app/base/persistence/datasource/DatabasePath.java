package vn.org.thn.app.base.persistence.datasource;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * App-relative folder layout used by every service that pulls in this module's DB
 * bootstrapping: ./config, ./database (Flyway scripts, one sub-folder per DatabaseType),
 * ./data (e.g. the SQLite file). A service needing more app-specific folders (uploads,
 * recordings, ...) should define those itself rather than extend this class.
 */
public final class DatabasePath {

    public static final Path HOME = Paths.get("").toAbsolutePath();
    public static final Path CONFIG_DIR = HOME.resolve("config");
    public static final Path DATABASE_DIR = HOME.resolve("database");
    public static final Path DATA_DIR = HOME.resolve("data");

    private DatabasePath() {
    }

    /** The Flyway migration folder for {@code databaseType}, e.g. {@code database/postgresql}. */
    public static Path migrationFolder(DatabaseType databaseType) {
        return DATABASE_DIR.resolve(databaseType.name().toLowerCase());
    }
}
