-- Check if registration_request table exists and show its schema
SELECT EXISTS (
   SELECT FROM information_schema.tables 
   WHERE table_schema = 'public' 
   AND table_name = 'registration_request'
) as table_exists;

-- If table exists, show the schema
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'registration_request'
ORDER BY ordinal_position;

-- Check migration history
SELECT version, description, type, installed_on, success
FROM flyway_schema_history
ORDER BY installed_on DESC
LIMIT 15;
