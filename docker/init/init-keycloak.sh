#!/bin/sh

# Configuration variables
KEYCLOAK_URL=http://keycloak_bank:8080
ADMIN_USER=admin
ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
REALM_NAME=spring-boot-realm-dev
CLIENT_ID=auth-service
REALM_ROLE_1=admin_role_realm
REALM_ROLE_2=user_role_realm

# Install required tools
apk add --no-cache curl jq

# Get admin token
get_token() {
    curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
         -d "grant_type=password" \
         -d "client_id=admin-cli" \
         -d "username=${ADMIN_USER}" \
         -d "password=${ADMIN_PASSWORD}" | jq -r '.access_token'
}

TOKEN=$(get_token)

# Create realm if not exists
echo "Checking if the realm \"${REALM_NAME}\" exists..."
if curl -s -o /dev/null -w "%{http_code}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}" \
    -H "Authorization: Bearer ${TOKEN}" | grep -q "200"; then
    echo "The realm \"${REALM_NAME}\" already exists."
else
    echo "Creating the realm \"${REALM_NAME}\"..."
    curl -X POST "${KEYCLOAK_URL}/admin/realms" \
         -H "Authorization: Bearer ${TOKEN}" \
         -H "Content-Type: application/json" \
         -d "{\"realm\":\"${REALM_NAME}\",\"enabled\":true}"
fi

# Create client if not exists
echo "Checking if the client \"${CLIENT_ID}\" exists..."
if curl -s "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
    -H "Authorization: Bearer ${TOKEN}" | grep -q "\"clientId\":\"${CLIENT_ID}\""; then
    echo "The client \"${CLIENT_ID}\" already exists."
else
    echo "Creating the client \"${CLIENT_ID}\"..."
    curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
         -H "Authorization: Bearer ${TOKEN}" \
         -H "Content-Type: application/json" \
         -d "{
             \"clientId\":\"${CLIENT_ID}\",
             \"name\":\"My New Client\",
             \"enabled\":true,
             \"protocol\":\"openid-connect\",
             \"publicClient\":true,
             \"redirectUris\":[\"${KEYCLOAK_URL}/*\"]
         }"
fi

# Create realm roles if not exist
for ROLE in "${REALM_ROLE_1}" "${REALM_ROLE_2}"; do
    echo "Checking if the realm role \"${ROLE}\" exists..."
    if curl -s "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
        -H "Authorization: Bearer ${TOKEN}" | grep -q "\"name\":\"${ROLE}\""; then
        echo "The realm role \"${ROLE}\" already exists."
    else
        echo "Creating the realm role \"${ROLE}\"..."
        curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
             -H "Authorization: Bearer ${TOKEN}" \
             -H "Content-Type: application/json" \
             -d "{\"name\":\"${ROLE}\"}"
    fi
done

echo "Initial Keycloak configuration completed."