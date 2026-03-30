-- Legacy schema compatibility:
-- old databases may still have users.name as NOT NULL from V3.
-- New registration uses username/email/password and does not write name.
ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
