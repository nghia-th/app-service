CREATE TABLE IF NOT EXISTS tbl_user
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT NOT NULL UNIQUE,
    email      TEXT,
    full_name  TEXT,
    status     TEXT DEFAULT 'ACTIVE',
    role       TEXT DEFAULT 'USER',
    created_at TEXT,
    updated_at TEXT,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN DEFAULT 0
);
