-- $Revision$
-- $Date$

ALTER TABLE duality ALTER COLUMN id INT IDENTITY;
ALTER TABLE duality ALTER COLUMN sender SET NOT NULL;
ALTER TABLE duality ALTER COLUMN receiver SET NOT NULL;
ALTER TABLE duality ALTER COLUMN time SET NOT NULL;
ALTER TABLE duality ALTER COLUMN message SET NOT NULL;

-- Update database version
UPDATE jiveVersion SET version = 2 WHERE name = 'duality';