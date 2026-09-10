CREATE TABLE IF NOT EXISTS tbl_user
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    username  TEXT NOT NULL UNIQUE,
    email     TEXT,
    full_name TEXT,
    status    TEXT DEFAULT 'ACTIVE',
    role      TEXT DEFAULT 'USER'
);
