package com.api.rest.util;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;

public class KeyCloakProvider {

    private static final String SERVER_KEYCLOAK_URL = "http://localhost:8282";
    private static final String REALM_NAME = "spring-boot-realm-dev";
    private static final String REALM_MASTER = "master";
    private static final String ADMIN_CLI = "admin-cli";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CLIENT_SECRET = "xt22qmKwGFIHbOpZpk0yhuwHI9i1S075";

    public static RealmResource getRealmResouce() {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(SERVER_KEYCLOAK_URL)
                .realm(REALM_MASTER)
                .clientId(ADMIN_CLI)
                .username(ADMIN_USERNAME)
                .password(ADMIN_PASSWORD)
                /*.clientSecret(CLIENT_SECRET)
                .resteasyClient(
                        new ResteasyClientBuilderImpl()
                                .connectionPoolSize(10)
                                .build()
                )*/
                .grantType(OAuth2Constants.PASSWORD)
                .build();

        return keycloak.realm(REALM_NAME);
    }

    public static UsersResource getUserResource() {
        return getRealmResouce().users();
    }



}
