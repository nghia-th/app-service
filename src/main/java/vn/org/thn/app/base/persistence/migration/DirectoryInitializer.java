package vn.org.thn.app.base.persistence.migration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.persistence.datasource.DatabasePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/** Creates the app-relative folders {@link DatabasePath} defines (config/, database/, data/) once at startup, if missing. */
@Component
public class DirectoryInitializer {

    /** Runs once at application startup. */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(DatabasePath.CONFIG_DIR);
            Files.createDirectories(DatabasePath.DATABASE_DIR);
            Files.createDirectories(DatabasePath.DATA_DIR);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create base app directories", e);
        }
    }
}
