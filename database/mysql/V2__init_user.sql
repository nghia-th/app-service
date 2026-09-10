CREATE TABLE IF NOT EXISTS tbl_user
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(100) NOT NULL UNIQUE,
    email     VARCHAR(150),
    full_name VARCHAR(150),
    status    VARCHAR(50) DEFAULT 'ACTIVE',
    role      VARCHAR(50) DEFAULT 'USER'
);
