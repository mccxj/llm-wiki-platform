-- Drop unused content_vector column from pages table
-- This column was added to the JPA entity but never used; it was not migrated to the database schema.
ALTER TABLE pages DROP COLUMN IF EXISTS content_vector;
