-- One-time provisioning. Run with the migration owner, never the application account.
-- psql variables: issuer, subject, display_name. No passwords or automatic first-login grants.
\set ON_ERROR_STOP on
BEGIN;
SELECT pg_advisory_xact_lock(720231100);
SELECT set_config('ribolov.bootstrap_issuer', :'issuer', true),
       set_config('ribolov.bootstrap_subject', :'subject', true),
       set_config('ribolov.bootstrap_name', :'display_name', true);
DO $$
DECLARE administrator uuid; assignment uuid;
BEGIN
    IF EXISTS (SELECT 1 FROM access.role_assignment WHERE role = 'SYSTEM_ADMIN') THEN
        RAISE EXCEPTION 'A system administrator has already been provisioned';
    END IF;
    IF length(btrim(current_setting('ribolov.bootstrap_subject'))) = 0 OR
       length(btrim(current_setting('ribolov.bootstrap_name'))) = 0 THEN
        RAISE EXCEPTION 'Verified subject and display name are required';
    END IF;
    INSERT INTO access.application_user(id, issuer, subject, display_name, active)
    VALUES (gen_random_uuid(), current_setting('ribolov.bootstrap_issuer'),
        current_setting('ribolov.bootstrap_subject'), current_setting('ribolov.bootstrap_name'), true)
    ON CONFLICT (issuer, subject) DO UPDATE SET active = true, display_name = EXCLUDED.display_name
    RETURNING id INTO administrator;
    INSERT INTO access.role_assignment(id, user_id, role, valid_from)
    VALUES (gen_random_uuid(), administrator, 'SYSTEM_ADMIN', CURRENT_TIMESTAMP)
    RETURNING id INTO assignment;
    INSERT INTO access.administration_event(id, occurred_at, actor_id, target_user_id, assignment_id, action, reason)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, administrator, administrator, assignment, 'BOOTSTRAP_ADMIN', 'Početno odobreno postavljanje administratora');
END $$;
COMMIT;
