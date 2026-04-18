CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    phone VARCHAR(32),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_address (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    consignee_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL
);

CREATE TABLE permission (
    id BIGINT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL
);

CREATE TABLE user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);
