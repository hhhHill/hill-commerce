-- Default admin seed for local development and initial bootstrap.
-- Password source text: Admin@123456
-- Current hash strategy: SHA-256 hex placeholder for bootstrap only.
-- Task 3 must align the runtime password encoder and rotate this seed if needed.

INSERT INTO users (email, password_hash, nickname, status)
VALUES (
    'admin@hill-commerce.local',
    SHA2('Admin@123456', 256),
    'System Admin',
    'ACTIVE'
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.email = 'admin@hill-commerce.local';
