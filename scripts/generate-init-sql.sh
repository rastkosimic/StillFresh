#!/usr/bin/env bash
# Generate root init-*.sql files from db-init/templates using values from .env.
# Existing Postgres volumes keep their original passwords until recreated
# (docker-compose down -v). Re-run whenever you change DB passwords in .env.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/.env"
TEMPLATE_DIR="$ROOT/db-init/templates"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing .env at $ENV_FILE. Copy .env.example to .env and fill in values first." >&2
  exit 1
fi

# Load KEY=VALUE from .env into the environment (safe subset)
while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line#"${line%%[![:space:]]*}"}"
  [[ -z "$line" || "$line" == \#* ]] && continue
  key="${line%%=*}"
  val="${line#*=}"
  key="$(echo "$key" | xargs)"
  case "$key" in
    POSTGRES_PASSWORD_*)
      # Strip surrounding quotes
      if [[ "$val" =~ ^\".*\"$ || "$val" =~ ^\'.*\'$ ]]; then
        val="${val:1:${#val}-2}"
      fi
      export "$key=$val"
      ;;
  esac
done < "$ENV_FILE"

sql_escape() {
  # Escape single quotes for SQL string literals
  printf '%s' "${1//\'/\'\'}"
}

require_env() {
  local name="$1"
  local value="${!name-}"
  if [[ -z "${value}" ]]; then
    echo "Required variable $name is missing or empty in .env" >&2
    exit 1
  fi
  sql_escape "$value"
}

PW_USER="$(require_env POSTGRES_PASSWORD_USER)"
PW_VENDOR="$(require_env POSTGRES_PASSWORD_VENDOR)"
PW_AUTH="$(require_env POSTGRES_PASSWORD_AUTH)"
PW_OFFERS="$(require_env POSTGRES_PASSWORD_OFFERS)"
PW_ORDERS="$(require_env POSTGRES_PASSWORD_ORDERS)"
PW_PAYMENTS="$(require_env POSTGRES_PASSWORD_PAYMENTS)"
PW_NOTIFICATION="$(require_env POSTGRES_PASSWORD_NOTIFICATION)"

substitute() {
  local tmpl="$1"
  local out="$2"
  sed \
    -e "s/__POSTGRES_PASSWORD_USER__/${PW_USER//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_VENDOR__/${PW_VENDOR//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_AUTH__/${PW_AUTH//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_OFFERS__/${PW_OFFERS//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_ORDERS__/${PW_ORDERS//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_PAYMENTS__/${PW_PAYMENTS//\//\\/}/g" \
    -e "s/__POSTGRES_PASSWORD_NOTIFICATION__/${PW_NOTIFICATION//\//\\/}/g" \
    "$tmpl" > "$out"
  if grep -q "__POSTGRES_PASSWORD_" "$out"; then
    echo "Unresolved password placeholder remaining in $out" >&2
    exit 1
  fi
  echo "Wrote $(basename "$out")"
}

for name in init-user init-vendor init-auth init-offer init-order init-payment init-notification; do
  substitute "$TEMPLATE_DIR/${name}.sql.template" "$ROOT/${name}.sql"
done

echo "Done. Generated init-*.sql are gitignored; commit only db-init/templates/."
