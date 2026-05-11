-- Align the bootstrap admin account with the runtime BCrypt-only password policy.
-- Password source text: Admin@123456

UPDATE users
SET password_hash = '$2a$10$dI.XZkJ4jiEunqTzzo5Eb.2OVbjfS0DoZxfmhZLCgAtRfbAAahvTq'
WHERE email = 'admin@hill-commerce.local';
