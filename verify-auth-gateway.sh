#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

echo "========================================"
echo " Auth via API Gateway E2E Verification"
echo "========================================"
echo "Base URL: ${BASE_URL}"
echo

EMAIL="gateway.e2e.$(date +%s)$RANDOM@example.com"
PASSWORD="Password1!"

json_get() {
  local key="$1"
  python - "$key" <<'PY'
import json, sys
key = sys.argv[1]
obj = json.load(sys.stdin)
print(obj.get(key, ""))
PY
}

echo "[1/7] Register via gateway"
REG_RESP=$(curl -sS -X POST "${BASE_URL}/auth/register" -H 'Content-Type: application/json' -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"fullName\":\"Gateway E2E User\"}")
ACCESS_TOKEN=$(echo "${REG_RESP}" | json_get accessToken)
if [[ -z "${ACCESS_TOKEN}" ]]; then
  ACCESS_TOKEN=$(echo "${REG_RESP}" | json_get token)
fi
REFRESH_TOKEN=$(echo "${REG_RESP}" | json_get refreshToken)

if [[ -z "${ACCESS_TOKEN}" || -z "${REFRESH_TOKEN}" ]]; then
  echo "Registration did not return expected tokens"
  echo "Response: ${REG_RESP}"
  exit 1
fi

echo "[2/7] Read profile via /auth/users/me"
ME_RESP=$(curl -sS -X GET "${BASE_URL}/auth/users/me" -H "Authorization: Bearer ${ACCESS_TOKEN}")
ME_EMAIL=$(echo "${ME_RESP}" | json_get email)
if [[ "${ME_EMAIL}" != "${EMAIL}" ]]; then
  echo "Profile email mismatch"
  echo "Response: ${ME_RESP}"
  exit 1
fi

echo "[3/7] Update profile with exactly 3 addresses"
PROFILE_CODE=$(curl -sS -o /tmp/profile_resp.json -w "%{http_code}" -X PUT "${BASE_URL}/auth/users/profile" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"phone":"0712345678","addresses":[{"street":"No 1","city":"Colombo","postalCode":"10100","isDefault":true},{"street":"No 2","city":"Kandy","postalCode":"20200","isDefault":false},{"street":"No 3","city":"Galle","postalCode":"30300","isDefault":false}]}' )
if [[ "${PROFILE_CODE}" != "200" ]]; then
  echo "Profile update failed with status ${PROFILE_CODE}"
  cat /tmp/profile_resp.json
  exit 1
fi

echo "[4/7] Ensure >3 addresses is rejected"
OVER_LIMIT_CODE=$(curl -sS -o /tmp/profile_overlimit.json -w "%{http_code}" -X PUT "${BASE_URL}/auth/users/profile" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"addresses":[{"street":"No 1","city":"Colombo","postalCode":"10100","isDefault":true},{"street":"No 2","city":"Kandy","postalCode":"20200","isDefault":false},{"street":"No 3","city":"Galle","postalCode":"30300","isDefault":false},{"street":"No 4","city":"Jaffna","postalCode":"40400","isDefault":false}]}' )
if [[ "${OVER_LIMIT_CODE}" != "400" ]]; then
  echo "Expected 400 for >3 addresses, got ${OVER_LIMIT_CODE}"
  cat /tmp/profile_overlimit.json
  exit 1
fi

echo "[5/7] Refresh access token"
REFRESH_RESP=$(curl -sS -X POST "${BASE_URL}/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"${REFRESH_TOKEN}\"}")
NEW_TOKEN=$(echo "${REFRESH_RESP}" | json_get accessToken)
if [[ -z "${NEW_TOKEN}" ]]; then
  NEW_TOKEN=$(echo "${REFRESH_RESP}" | json_get token)
fi
if [[ -z "${NEW_TOKEN}" ]]; then
  echo "Refresh did not return a new token"
  echo "Response: ${REFRESH_RESP}"
  exit 1
fi

echo "[6/7] Logout with refreshed token"
LOGOUT_CODE=$(curl -sS -o /tmp/logout_resp.json -w "%{http_code}" -X POST "${BASE_URL}/auth/logout" \
  -H "Authorization: Bearer ${NEW_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"${REFRESH_TOKEN}\"}")
if [[ "${LOGOUT_CODE}" != "200" ]]; then
  echo "Logout failed with status ${LOGOUT_CODE}"
  cat /tmp/logout_resp.json
  exit 1
fi

echo "[7/7] Ensure refresh token is revoked after logout"
REUSE_CODE=$(curl -sS -o /tmp/reuse_resp.json -w "%{http_code}" -X POST "${BASE_URL}/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"${REFRESH_TOKEN}\"}")
if [[ "${REUSE_CODE}" != "401" ]]; then
  echo "Expected 401 for refresh reuse after logout, got ${REUSE_CODE}"
  cat /tmp/reuse_resp.json
  exit 1
fi

echo
echo "========================================"
echo " Gateway E2E Verification PASSED ✓"
echo "========================================"
echo "Test user: ${EMAIL}"
echo