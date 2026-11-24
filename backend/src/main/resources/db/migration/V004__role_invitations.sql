CREATE TABLE access.role_invitation (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    email varchar(254) NOT NULL CHECK (email=lower(btrim(email))),
    role varchar(40) NOT NULL CHECK (role IN ('MINISTRY_ANALYST','FEDERATION_ANALYST','ASSOCIATION_ADMIN','OPERATOR','WARDEN')),
    organization_id uuid NOT NULL REFERENCES organizations.organization(id),
    invited_by_id uuid NOT NULL REFERENCES access.role_assignment(id),
    accepted_assignment_id uuid UNIQUE REFERENCES access.role_assignment(id),
    token_hash varchar(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at = created_at + interval '48 hours'),
    valid_until timestamptz CHECK (valid_until IS NULL OR valid_until > expires_at),
    accepted_at timestamptz,
    revoked_at timestamptz,
    sent_at timestamptz,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at timestamptz NOT NULL,
    delivery_failure varchar(30),
    CHECK ((accepted_assignment_id IS NULL) = (accepted_at IS NULL)),
    CHECK (accepted_at IS NULL OR revoked_at IS NULL)
);
CREATE INDEX ix_invitation_org_created ON access.role_invitation(organization_id,created_at DESC);
CREATE INDEX ix_invitation_delivery ON access.role_invitation(next_attempt_at) WHERE sent_at IS NULL AND revoked_at IS NULL AND accepted_at IS NULL;
ALTER TABLE access.administration_event ADD COLUMN invitation_id uuid REFERENCES access.role_invitation(id);
ALTER TABLE access.role_invitation ENABLE ROW LEVEL SECURITY;
REVOKE DELETE,UPDATE ON access.role_invitation FROM ribolov_app;
GRANT UPDATE(version,accepted_assignment_id,accepted_at,revoked_at,sent_at,attempts,next_attempt_at,delivery_failure) ON access.role_invitation TO ribolov_app;

CREATE FUNCTION access.invitation_recipient(target uuid, pending boolean DEFAULT false) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
    SELECT EXISTS(SELECT 1 FROM access.role_invitation i WHERE i.id=target
        AND i.token_hash=nullif(current_setting('ribolov.invitation_hash',true),'')
        AND i.email=nullif(current_setting('ribolov.verified_email',true),'')
        AND nullif(current_setting('ribolov.issuer',true),'') IS NOT NULL
        AND nullif(current_setting('ribolov.subject',true),'') IS NOT NULL
        AND current_setting('ribolov.acr',true)='2' AND access.admin_is_fresh()
        AND (NOT pending OR (i.accepted_at IS NULL AND i.revoked_at IS NULL AND CURRENT_TIMESTAMP < i.expires_at)))
$$;
CREATE FUNCTION access.invitation_link_visible(target uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
    SELECT access.invitation_recipient(target,false)
       OR (current_setting('ribolov.invitation_worker',true)='true'
           AND target=nullif(current_setting('ribolov.delivery_invitation_id',true),'')::uuid)
$$;
CREATE FUNCTION access.invitation_author_valid(target uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM access.role_invitation i
    JOIN access.role_assignment r ON r.id=i.invited_by_id JOIN access.application_user u ON u.id=r.user_id
    JOIN organizations.organization o ON o.id=i.organization_id
    WHERE i.id=target AND u.active AND o.status='ACTIVE'
      AND r.valid_from<=CURRENT_TIMESTAMP AND (r.valid_until IS NULL OR CURRENT_TIMESTAMP<r.valid_until)
      AND (r.revoked_at IS NULL OR CURRENT_TIMESTAMP<r.revoked_at)
      AND ((r.role='SYSTEM_ADMIN' AND i.role IN ('MINISTRY_ANALYST','FEDERATION_ANALYST','ASSOCIATION_ADMIN'))
       OR (r.role='ASSOCIATION_ADMIN' AND r.organization_id=i.organization_id AND i.role IN ('OPERATOR','WARDEN'))))
$$;
CREATE FUNCTION access.invitation_user_visible(target uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM access.role_invitation i JOIN access.role_assignment r ON r.id=i.invited_by_id
    WHERE access.invitation_link_visible(i.id) AND (r.user_id=target OR EXISTS(
        SELECT 1 FROM access.application_user u WHERE u.id=target AND u.issuer=current_setting('ribolov.issuer',true)
        AND u.subject=current_setting('ribolov.subject',true))))
$$;
CREATE FUNCTION access.invitation_role_visible(target uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM access.role_invitation i WHERE access.invitation_link_visible(i.id)
    AND (i.invited_by_id=target OR i.accepted_assignment_id=target))
$$;
CREATE FUNCTION access.invitation_org_visible(target uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM access.role_invitation i WHERE i.organization_id=target AND access.invitation_link_visible(i.id))
$$;
CREATE FUNCTION access.invitation_grant_allowed(target_user uuid,target_org uuid,target_role varchar,ends timestamptz) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM access.role_invitation i JOIN access.application_user u ON u.id=target_user
    WHERE access.invitation_recipient(i.id,true) AND access.invitation_author_valid(i.id)
      AND i.organization_id=target_org AND i.role=target_role AND i.valid_until IS NOT DISTINCT FROM ends
      AND u.active AND u.issuer=current_setting('ribolov.issuer',true) AND u.subject=current_setting('ribolov.subject',true))
$$;
REVOKE ALL ON FUNCTION access.invitation_recipient(uuid,boolean),access.invitation_link_visible(uuid),
 access.invitation_author_valid(uuid),access.invitation_user_visible(uuid),access.invitation_role_visible(uuid),
 access.invitation_org_visible(uuid),access.invitation_grant_allowed(uuid,uuid,varchar,timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION access.invitation_recipient(uuid,boolean),access.invitation_link_visible(uuid),
 access.invitation_author_valid(uuid),access.invitation_user_visible(uuid),access.invitation_role_visible(uuid),
 access.invitation_org_visible(uuid),access.invitation_grant_allowed(uuid,uuid,varchar,timestamptz) TO ribolov_app;

CREATE POLICY invitation_read ON access.role_invitation FOR SELECT TO ribolov_app
USING (access.admin_is_system() OR EXISTS(SELECT 1 FROM access.admin_context() c WHERE c.organization_id=role_invitation.organization_id)
 OR access.invitation_recipient(id,false) OR current_setting('ribolov.invitation_worker',true)='true');
CREATE POLICY invitation_insert ON access.role_invitation FOR INSERT TO ribolov_app
WITH CHECK (access.admin_is_fresh() AND access.admin_can_manage_role(role,organization_id)
 AND invited_by_id=nullif(current_setting('ribolov.assignment_id',true),'')::uuid);
CREATE POLICY invitation_update ON access.role_invitation FOR UPDATE TO ribolov_app
USING ((access.admin_is_fresh() AND access.admin_can_manage_role(role,organization_id))
 OR access.invitation_recipient(id,false) OR current_setting('ribolov.invitation_worker',true)='true')
WITH CHECK ((access.admin_is_fresh() AND access.admin_can_manage_role(role,organization_id))
 OR access.invitation_recipient(id,false) OR current_setting('ribolov.invitation_worker',true)='true');

CREATE POLICY invitation_user_read ON access.application_user FOR SELECT TO ribolov_app USING (access.invitation_user_visible(id));
CREATE POLICY invitation_user_insert ON access.application_user FOR INSERT TO ribolov_app WITH CHECK (
 issuer=current_setting('ribolov.issuer',true) AND subject=current_setting('ribolov.subject',true) AND active
 AND EXISTS(SELECT 1 FROM access.role_invitation i WHERE access.invitation_recipient(i.id,true) AND access.invitation_author_valid(i.id)));
CREATE POLICY invitation_org_read ON organizations.organization FOR SELECT TO ribolov_app USING (access.invitation_org_visible(id));
CREATE POLICY invitation_role_read ON access.role_assignment FOR SELECT TO ribolov_app USING (access.invitation_role_visible(id));
CREATE POLICY invitation_role_insert ON access.role_assignment FOR INSERT TO ribolov_app
WITH CHECK (access.invitation_grant_allowed(user_id,organization_id,role,valid_until) AND revoked_at IS NULL
 AND valid_from BETWEEN CURRENT_TIMESTAMP AND clock_timestamp()+interval '30 seconds');

-- Reading a linked inviter must not expose their unrelated grants.
DROP POLICY own_roles ON access.role_assignment;
CREATE POLICY own_roles ON access.role_assignment FOR SELECT TO ribolov_app USING (
 user_id IN (SELECT id FROM access.application_user WHERE issuer=current_setting('ribolov.issuer',true) AND subject=current_setting('ribolov.subject',true) AND active)
 AND valid_from<=CURRENT_TIMESTAMP AND (valid_until IS NULL OR CURRENT_TIMESTAMP<valid_until)
 AND (revoked_at IS NULL OR CURRENT_TIMESTAMP<revoked_at)
 AND (organization_id IS NULL OR access.can_read_organization(organization_id)));

CREATE POLICY invitation_event_insert ON access.administration_event FOR INSERT TO ribolov_app WITH CHECK (
 action='INVITATION_ACCEPTED' AND access.invitation_recipient(invitation_id,false)
 AND actor_id=target_user_id AND EXISTS(SELECT 1 FROM access.role_invitation i JOIN access.role_assignment r ON r.id=i.accepted_assignment_id
    JOIN access.application_user u ON u.id=r.user_id
    WHERE i.id=administration_event.invitation_id AND i.organization_id=administration_event.organization_id
      AND r.id=administration_event.assignment_id AND u.id=administration_event.actor_id
      AND u.issuer=current_setting('ribolov.issuer',true) AND u.subject=current_setting('ribolov.subject',true)));

CREATE FUNCTION access.check_invitation_transition() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF session_user<>'ribolov_app' THEN RETURN NEW; END IF;
 IF NEW.accepted_assignment_id IS DISTINCT FROM OLD.accepted_assignment_id OR NEW.accepted_at IS DISTINCT FROM OLD.accepted_at THEN
    IF OLD.accepted_at IS NOT NULL OR OLD.revoked_at IS NOT NULL OR OLD.expires_at<=CURRENT_TIMESTAMP
       OR NEW.accepted_at IS NULL OR NOT access.invitation_recipient(OLD.id,true)
       OR NOT EXISTS(SELECT 1 FROM access.role_assignment r WHERE r.id=NEW.accepted_assignment_id
            AND access.invitation_grant_allowed(r.user_id,r.organization_id,r.role,r.valid_until)) THEN
        RAISE EXCEPTION 'Invalid invitation acceptance' USING ERRCODE='42501';
    END IF;
 END IF;
 IF NEW.revoked_at IS DISTINCT FROM OLD.revoked_at THEN
    IF OLD.revoked_at IS NOT NULL OR OLD.accepted_at IS NOT NULL OR NEW.revoked_at IS NULL
       OR NOT access.admin_is_fresh() OR NOT access.admin_can_manage_role(OLD.role,OLD.organization_id) THEN
        RAISE EXCEPTION 'Invalid invitation revocation' USING ERRCODE='42501';
    END IF;
 END IF;
 IF (NEW.sent_at,NEW.attempts,NEW.next_attempt_at,NEW.delivery_failure)
       IS DISTINCT FROM (OLD.sent_at,OLD.attempts,OLD.next_attempt_at,OLD.delivery_failure) THEN
    IF current_setting('ribolov.invitation_worker',true) IS DISTINCT FROM 'true'
      AND NOT (access.admin_is_fresh() AND access.admin_can_manage_role(OLD.role,OLD.organization_id)) THEN
        RAISE EXCEPTION 'Invalid invitation delivery change' USING ERRCODE='42501';
    END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER check_invitation_transition BEFORE UPDATE ON access.role_invitation FOR EACH ROW EXECUTE FUNCTION access.check_invitation_transition();
