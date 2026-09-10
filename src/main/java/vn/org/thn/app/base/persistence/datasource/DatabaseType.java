package vn.org.thn.app.base.persistence.datasource;

/** The database engines this module has a {@link DatabaseProvider}, dialect, and Flyway migration folder for. */
public enum DatabaseType {
    /** Embedded SQLite (single-file, no server). */
    SQLITE,
    /** PostgreSQL. */
    POSTGRESQL,
    /** Microsoft SQL Server. */
    SQLSERVER,
    /** MySQL / compatible (e.g. MariaDB). */
    MYSQL,
    /** Oracle 12c and newer. */
    ORACLE
}
