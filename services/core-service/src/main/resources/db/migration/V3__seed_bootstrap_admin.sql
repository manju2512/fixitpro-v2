-- V3__seed_bootstrap_admin.sql
-- A single bootstrap admin account, since public signup only ever creates
-- CUSTOMER accounts (by design - see AuthService.signup) and technician
-- accounts can only be created by an authenticated admin. Without this seed
-- there would be no way to create the very first admin.
--
-- Login: username "admin", password "ChangeMe123"
-- CHANGE THIS PASSWORD IMMEDIATELY after first login in any real deployment.

INSERT INTO users (username, email, password_hash, phone, role_id, is_active)
SELECT 'admin', 'admin@fixitpro.local', '$2b$12$zwBve3.zo0iHSTb/jRz.P.bEQLCNQcnDJOTXNni9jDZ/vwp4F8ShO', NULL, r.role_id, TRUE
FROM role r WHERE r.name = 'ADMIN';
