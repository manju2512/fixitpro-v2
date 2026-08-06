-- V4__add_phone_unique_constraint.sql
-- Phone became a valid login identifier (alongside username/email) once
-- flexible login shipped - for that lookup to safely resolve to exactly one
-- account, phone must be unique the same way email already is. NULL stays
-- allowed (the bootstrap admin has phone = NULL per V3) since MySQL treats
-- multiple NULLs as non-conflicting in a UNIQUE index, so this won't break
-- that seed row.
--
-- IMPORTANT: this migration will FAIL to apply if the target database
-- currently has any duplicate non-null phone values - check first with:
--   SELECT phone, COUNT(*) FROM users WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;
-- If that returns any rows, resolve them (update the duplicates to unique
-- values, or NULL out the extras) before this migration runs, or Flyway
-- will fail on startup and core-service won't come up.

ALTER TABLE users ADD CONSTRAINT uq_users_phone UNIQUE (phone);
