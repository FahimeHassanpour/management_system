ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(100);

UPDATE users
SET username = COALESCE(NULLIF(username, ''), split_part(email, '@', 1))
WHERE username IS NULL OR username = '';

ALTER TABLE users ALTER COLUMN username SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_users_username'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
    END IF;
END $$;
