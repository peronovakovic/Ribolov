-- The deployment/migration account owns schemas. The runtime account cannot perform DDL.
CREATE SCHEMA IF NOT EXISTS access;
CREATE SCHEMA IF NOT EXISTS people;
CREATE SCHEMA IF NOT EXISTS membership;
CREATE SCHEMA IF NOT EXISTS waters;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS permits;
CREATE SCHEMA IF NOT EXISTS fieldwork;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS documents;
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE SCHEMA IF NOT EXISTS audit;

DO $$
DECLARE module_schema text;
BEGIN
    FOREACH module_schema IN ARRAY ARRAY['access', 'people', 'membership', 'waters', 'billing',
        'permits', 'fieldwork', 'reporting', 'documents', 'notifications', 'audit']
    LOOP
        EXECUTE format('GRANT USAGE ON SCHEMA %I TO ribolov_app', module_schema);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ribolov_app', module_schema);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE, SELECT ON SEQUENCES TO ribolov_app', module_schema);
    END LOOP;
END $$;

-- Row-level policies arrive with each domain table; no domain data is exposed by this migration.
