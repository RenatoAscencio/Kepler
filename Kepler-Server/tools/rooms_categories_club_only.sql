ALTER TABLE rooms_categories ADD COLUMN IF NOT EXISTS club_only TINYINT(1) DEFAULT 0;
UPDATE rooms_categories SET club_only = 1 WHERE id = 8;
