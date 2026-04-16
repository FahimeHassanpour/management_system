UPDATE password_entries
SET category_id = 1
WHERE category_id IS NULL;

ALTER TABLE password_entries
    ALTER COLUMN category_id SET NOT NULL;