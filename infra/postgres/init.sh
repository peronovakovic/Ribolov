#!/usr/bin/env bash
set -euo pipefail
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 \
  --set app_password="$RIBOLOV_APP_PASSWORD" --set keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE ribolov_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L', :'app_password') \gexec
SELECT format('CREATE ROLE keycloak_owner LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L', :'keycloak_password') \gexec
GRANT CONNECT ON DATABASE ribolov TO ribolov_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE DATABASE keycloak OWNER keycloak_owner;
SQL