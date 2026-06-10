-- Merge user assignments from duplicate password entries onto the oldest row per title+username.
INSERT INTO assignments (user_id, password_entry_id, assigned_at)
SELECT DISTINCT a.user_id, dup.canonical_id, a.assigned_at
FROM (
    SELECT id, MIN(id) OVER (PARTITION BY title, username) AS canonical_id
    FROM password_entries
) dup
JOIN assignments a ON a.password_entry_id = dup.id
WHERE dup.id <> dup.canonical_id
  AND NOT EXISTS (
    SELECT 1
    FROM assignments existing
    WHERE existing.user_id = a.user_id
      AND existing.password_entry_id = dup.canonical_id
  );

DELETE FROM assignments
WHERE password_entry_id IN (
    SELECT p.id
    FROM password_entries p
    WHERE p.id <> (
        SELECT MIN(p2.id)
        FROM password_entries p2
        WHERE p2.title = p.title AND p2.username = p.username
    )
);

-- Merge team assignments from duplicate password entries onto the oldest row per title+username.
INSERT INTO team_password_assignments (team_id, password_id)
SELECT DISTINCT tpa.team_id, dup.canonical_id
FROM (
    SELECT id, MIN(id) OVER (PARTITION BY title, username) AS canonical_id
    FROM password_entries
) dup
JOIN team_password_assignments tpa ON tpa.password_id = dup.id
WHERE dup.id <> dup.canonical_id
  AND NOT EXISTS (
    SELECT 1
    FROM team_password_assignments existing
    WHERE existing.team_id = tpa.team_id
      AND existing.password_id = dup.canonical_id
  );

DELETE FROM team_password_assignments
WHERE password_id IN (
    SELECT p.id
    FROM password_entries p
    WHERE p.id <> (
        SELECT MIN(p2.id)
        FROM password_entries p2
        WHERE p2.title = p.title AND p2.username = p.username
    )
);

DELETE FROM password_entries
WHERE id NOT IN (
    SELECT MIN(id)
    FROM password_entries
    GROUP BY title, username
);

ALTER TABLE password_entries
    ADD CONSTRAINT uq_password_entries_title_username UNIQUE (title, username);
