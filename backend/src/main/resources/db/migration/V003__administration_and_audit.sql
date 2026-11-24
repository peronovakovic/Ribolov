ALTER TABLE access.application_user ADD COLUMN display_name varchar(240) NOT NULL DEFAULT 'Korisnik';

CREATE TABLE access.administration_event (
    id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL,
    actor_id uuid NOT NULL REFERENCES access.application_user(id),
    organization_id uuid REFERENCES organizations.organization(id),
    target_user_id uuid REFERENCES access.application_user(id),
    assignment_id uuid REFERENCES access.role_assignment(id),
    affiliation_id uuid REFERENCES organizations.organization_affiliation(id),
    action varchar(40) NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(btrim(reason)) > 0)
);
CREATE INDEX ix_administration_event_org_time ON access.administration_event(organization_id, occurred_at DESC);
ALTER TABLE access.administration_event ENABLE ROW LEVEL SECURITY;
REVOKE UPDATE, DELETE ON access.administration_event FROM ribolov_app;

CREATE FUNCTION access.admin_context()
RETURNS TABLE(user_id uuid, role varchar, organization_id uuid)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT u.id, r.role, r.organization_id
    FROM access.application_user u JOIN access.role_assignment r ON r.user_id = u.id
    LEFT JOIN organizations.organization o ON o.id = r.organization_id
    WHERE u.issuer = current_setting('ribolov.issuer', true)
      AND u.subject = current_setting('ribolov.subject', true) AND u.active
      AND r.id = nullif(current_setting('ribolov.assignment_id', true), '')::uuid
      AND current_setting('ribolov.acr', true) = '2'
      AND r.role IN ('SYSTEM_ADMIN', 'ASSOCIATION_ADMIN')
      AND r.valid_from <= CURRENT_TIMESTAMP
      AND (r.valid_until IS NULL OR CURRENT_TIMESTAMP < r.valid_until)
      AND (r.revoked_at IS NULL OR CURRENT_TIMESTAMP < r.revoked_at)
      AND (r.organization_id IS NULL OR o.status = 'ACTIVE')
$$;

CREATE FUNCTION access.admin_is_system() RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN')
$$;

CREATE FUNCTION access.admin_is_fresh() RETURNS boolean
LANGUAGE sql STABLE SET search_path = pg_catalog AS $$
    SELECT coalesce(to_timestamp(nullif(current_setting('ribolov.auth_time', true), '')::double precision)
            BETWEEN CURRENT_TIMESTAMP - interval '5 minutes' AND CURRENT_TIMESTAMP + interval '30 seconds', false)
$$;

CREATE FUNCTION access.admin_can_manage_role(target_role varchar, target_org uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT EXISTS (SELECT 1 FROM access.admin_context() c WHERE
        (c.role = 'SYSTEM_ADMIN' AND target_role IN ('MINISTRY_ANALYST', 'FEDERATION_ANALYST', 'ASSOCIATION_ADMIN'))
        OR (c.role = 'ASSOCIATION_ADMIN' AND target_org = c.organization_id AND target_role IN ('OPERATOR', 'WARDEN')))
$$;

CREATE FUNCTION access.admin_can_read_user(target_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN'
        OR target_id = c.user_id
        OR (c.role = 'ASSOCIATION_ADMIN' AND
            (EXISTS (SELECT 1 FROM access.role_assignment r WHERE r.user_id = target_id AND r.organization_id = c.organization_id)
             OR EXISTS (SELECT 1 FROM access.administration_event e WHERE e.actor_id = target_id AND e.organization_id = c.organization_id)
             OR (access.admin_is_fresh() AND target_id = nullif(current_setting('ribolov.target_user_id', true), '')::uuid))))
$$;

CREATE OR REPLACE FUNCTION access.can_read_organization(target_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT CASE WHEN nullif(current_setting('ribolov.assignment_id', true), '') IS NOT NULL THEN
        EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN' OR c.organization_id = target_id)
    ELSE EXISTS (
        SELECT 1 FROM access.application_user u
        JOIN access.role_assignment r ON r.user_id = u.id
        JOIN organizations.organization o ON o.id = target_id
        WHERE u.issuer = current_setting('ribolov.issuer', true) AND u.subject = current_setting('ribolov.subject', true)
          AND u.active AND o.status = 'ACTIVE' AND r.valid_from <= CURRENT_TIMESTAMP
          AND (r.valid_until IS NULL OR CURRENT_TIMESTAMP < r.valid_until)
          AND (r.revoked_at IS NULL OR CURRENT_TIMESTAMP < r.revoked_at)
          AND (r.organization_id = target_id OR r.role = 'SYSTEM_ADMIN'))
    END
$$;

REVOKE ALL ON FUNCTION access.admin_context(), access.admin_is_system(), access.admin_is_fresh(),
    access.admin_can_manage_role(varchar, uuid), access.admin_can_read_user(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION access.admin_context(), access.admin_is_system(), access.admin_is_fresh(),
    access.admin_can_manage_role(varchar, uuid), access.admin_can_read_user(uuid) TO ribolov_app;

CREATE POLICY admin_user_read ON access.application_user FOR SELECT TO ribolov_app
USING (access.admin_can_read_user(id));
CREATE POLICY admin_user_insert ON access.application_user FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_system() AND access.admin_is_fresh());
CREATE POLICY admin_user_update ON access.application_user FOR UPDATE TO ribolov_app
USING (access.admin_is_system() AND access.admin_is_fresh())
WITH CHECK (access.admin_is_system() AND access.admin_is_fresh());
REVOKE UPDATE ON access.application_user FROM ribolov_app;
GRANT UPDATE(display_name, active, version) ON access.application_user TO ribolov_app;

CREATE POLICY admin_role_read ON access.role_assignment FOR SELECT TO ribolov_app
USING (EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN' OR c.organization_id = role_assignment.organization_id));
CREATE POLICY admin_role_insert ON access.role_assignment FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_fresh() AND access.admin_can_manage_role(role, organization_id));
CREATE POLICY admin_role_update ON access.role_assignment FOR UPDATE TO ribolov_app
USING (access.admin_is_fresh() AND access.admin_can_manage_role(role, organization_id))
WITH CHECK (access.admin_is_fresh() AND access.admin_can_manage_role(role, organization_id));
REVOKE UPDATE ON access.role_assignment FROM ribolov_app;
GRANT UPDATE(revoked_at, version) ON access.role_assignment TO ribolov_app;

CREATE POLICY admin_organization_insert ON organizations.organization FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_system() AND access.admin_is_fresh());
CREATE POLICY admin_organization_update ON organizations.organization FOR UPDATE TO ribolov_app
USING (access.admin_is_fresh() AND EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN' OR c.organization_id = organization.id))
WITH CHECK (access.admin_is_fresh() AND EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN' OR c.organization_id = organization.id));
REVOKE UPDATE ON organizations.organization FROM ribolov_app;
GRANT UPDATE(name, contact_email, contact_phone, version) ON organizations.organization TO ribolov_app;

CREATE POLICY admin_affiliation_read ON organizations.organization_affiliation FOR SELECT TO ribolov_app
USING (access.admin_is_system());
CREATE POLICY admin_affiliation_insert ON organizations.organization_affiliation FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_system() AND access.admin_is_fresh());
CREATE POLICY admin_affiliation_update ON organizations.organization_affiliation FOR UPDATE TO ribolov_app
USING (access.admin_is_system() AND access.admin_is_fresh())
WITH CHECK (access.admin_is_system() AND access.admin_is_fresh());
REVOKE UPDATE ON organizations.organization_affiliation FROM ribolov_app;
GRANT UPDATE(valid_until, version) ON organizations.organization_affiliation TO ribolov_app;

CREATE POLICY admin_event_read ON access.administration_event FOR SELECT TO ribolov_app
USING (EXISTS (SELECT 1 FROM access.admin_context() c WHERE c.role = 'SYSTEM_ADMIN' OR c.organization_id = administration_event.organization_id));
CREATE POLICY admin_event_insert ON access.administration_event FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_fresh() AND EXISTS (SELECT 1 FROM access.admin_context() c
    WHERE c.user_id = administration_event.actor_id AND (c.role = 'SYSTEM_ADMIN' OR c.organization_id = administration_event.organization_id)));

-- Accounts holding a system role are maintained through controlled bootstrap/operations,
-- so the application cannot deactivate its last administrator.
CREATE FUNCTION access.protect_system_account() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog AS $$
BEGIN
    IF session_user = 'ribolov_app' AND NOT NEW.active
       AND EXISTS (SELECT 1 FROM access.role_assignment r WHERE r.user_id = NEW.id AND r.role = 'SYSTEM_ADMIN') THEN
        RAISE EXCEPTION 'System administrator cannot be deactivated through application' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER protect_system_account BEFORE UPDATE ON access.application_user
FOR EACH ROW EXECUTE FUNCTION access.protect_system_account();
