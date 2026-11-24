CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE SCHEMA organizations;
GRANT USAGE ON SCHEMA organizations TO ribolov_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA organizations GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ribolov_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA organizations GRANT USAGE, SELECT ON SEQUENCES TO ribolov_app;

CREATE TABLE organizations.organization (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    name varchar(240) NOT NULL CHECK (length(btrim(name)) > 0),
    type varchar(30) NOT NULL CHECK (type IN ('MINISTRY', 'FEDERATION', 'ASSOCIATION')),
    status varchar(30) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    contact_email varchar(254),
    contact_phone varchar(50)
);

CREATE TABLE organizations.organization_affiliation (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    child_id uuid NOT NULL,
    parent_id uuid NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_until timestamptz,
    CONSTRAINT fk_affiliation_child FOREIGN KEY (child_id) REFERENCES organizations.organization(id),
    CONSTRAINT fk_affiliation_parent FOREIGN KEY (parent_id) REFERENCES organizations.organization(id),
    CONSTRAINT ck_affiliation_different_organizations CHECK (child_id <> parent_id),
    CONSTRAINT ck_affiliation_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ex_affiliation_period EXCLUDE USING gist
        (child_id WITH =, tstzrange(valid_from, valid_until, '[)') WITH &&)
);
CREATE INDEX ix_affiliation_parent ON organizations.organization_affiliation(parent_id);

CREATE FUNCTION organizations.check_affiliation_types() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog AS $$
DECLARE child_type text; parent_type text;
BEGIN
    SELECT type INTO child_type FROM organizations.organization WHERE id = NEW.child_id;
    SELECT type INTO parent_type FROM organizations.organization WHERE id = NEW.parent_id;
    IF NOT ((child_type = 'FEDERATION' AND parent_type = 'MINISTRY')
         OR (child_type = 'ASSOCIATION' AND parent_type = 'FEDERATION')) THEN
        RAISE EXCEPTION 'Invalid organization hierarchy' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER check_affiliation_types BEFORE INSERT OR UPDATE
ON organizations.organization_affiliation FOR EACH ROW EXECUTE FUNCTION organizations.check_affiliation_types();

CREATE TABLE access.application_user (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    issuer varchar(500) NOT NULL CHECK (length(btrim(issuer)) > 0),
    subject varchar(255) NOT NULL CHECK (length(btrim(subject)) > 0),
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_user_identity UNIQUE (issuer, subject)
);

CREATE TABLE access.role_assignment (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    user_id uuid NOT NULL,
    organization_id uuid,
    role varchar(40) NOT NULL CHECK (role IN
        ('SYSTEM_ADMIN', 'MINISTRY_ANALYST', 'FEDERATION_ANALYST', 'ASSOCIATION_ADMIN', 'OPERATOR', 'WARDEN', 'ANGLER')),
    valid_from timestamptz NOT NULL,
    valid_until timestamptz,
    revoked_at timestamptz,
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES access.application_user(id),
    CONSTRAINT fk_role_organization FOREIGN KEY (organization_id) REFERENCES organizations.organization(id),
    CONSTRAINT ck_role_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_role_scope CHECK ((role IN ('SYSTEM_ADMIN', 'ANGLER')) = (organization_id IS NULL)),
    CONSTRAINT ex_role_period EXCLUDE USING gist
        (user_id WITH =, role WITH =,
         (coalesce(organization_id, '00000000-0000-0000-0000-000000000000'::uuid)) WITH =,
         tstzrange(valid_from, valid_until, '[)') WITH &&)
        WHERE (revoked_at IS NULL)
);
CREATE INDEX ix_role_organization ON access.role_assignment(organization_id);
CREATE INDEX ix_role_user ON access.role_assignment(user_id);

CREATE FUNCTION access.check_role_organization_type() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog AS $$
DECLARE organization_type text;
BEGIN
    IF NEW.organization_id IS NULL THEN RETURN NEW; END IF;
    SELECT type INTO organization_type FROM organizations.organization WHERE id = NEW.organization_id;
    IF NOT ((NEW.role = 'MINISTRY_ANALYST' AND organization_type = 'MINISTRY')
         OR (NEW.role = 'FEDERATION_ANALYST' AND organization_type = 'FEDERATION')
         OR (NEW.role IN ('ASSOCIATION_ADMIN', 'OPERATOR', 'WARDEN') AND organization_type = 'ASSOCIATION')) THEN
        RAISE EXCEPTION 'Role and organization scope do not match' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER check_role_organization_type BEFORE INSERT OR UPDATE
ON access.role_assignment FOR EACH ROW EXECUTE FUNCTION access.check_role_organization_type();

-- A trusted server sets identity only for the current transaction.
-- The function owner is the migration account, never the application runtime account.
CREATE FUNCTION access.can_read_organization(target_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog AS $$
    SELECT EXISTS (
        SELECT 1 FROM access.application_user u
        JOIN access.role_assignment r ON r.user_id = u.id
        JOIN organizations.organization o ON o.id = target_id
        WHERE u.issuer = current_setting('ribolov.issuer', true)
          AND u.subject = current_setting('ribolov.subject', true)
          AND u.active AND o.status = 'ACTIVE'
          AND r.valid_from <= CURRENT_TIMESTAMP
          AND (r.valid_until IS NULL OR CURRENT_TIMESTAMP < r.valid_until)
          AND (r.revoked_at IS NULL OR CURRENT_TIMESTAMP < r.revoked_at)
          AND (r.organization_id = target_id OR r.role = 'SYSTEM_ADMIN')
    )
$$;
REVOKE ALL ON FUNCTION access.can_read_organization(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION access.can_read_organization(uuid) TO ribolov_app;

ALTER TABLE access.application_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE access.role_assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations.organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations.organization_affiliation ENABLE ROW LEVEL SECURITY;

CREATE POLICY own_user ON access.application_user FOR SELECT TO ribolov_app
USING (issuer = current_setting('ribolov.issuer', true)
   AND subject = current_setting('ribolov.subject', true) AND active);

CREATE POLICY own_roles ON access.role_assignment FOR SELECT TO ribolov_app
USING (user_id IN (SELECT id FROM access.application_user)
   AND valid_from <= CURRENT_TIMESTAMP
   AND (valid_until IS NULL OR CURRENT_TIMESTAMP < valid_until)
   AND (revoked_at IS NULL OR CURRENT_TIMESTAMP < revoked_at)
   AND (organization_id IS NULL OR access.can_read_organization(organization_id)));

CREATE POLICY assigned_organizations ON organizations.organization FOR SELECT TO ribolov_app
USING (access.can_read_organization(id));

-- No application write policies or affiliation read policy yet.
-- Management use cases will introduce narrowly scoped policies with their authorization tests.
