package com.api.rest.service.impl;

import com.api.rest.controller.dto.UserDto;
import com.api.rest.service.IKeycloakService;
import com.api.rest.util.KeyCloakProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class KeycloakServiceImpl implements IKeycloakService {

    private static final String KC_USER_ROLE = "user_realm_role";


    @Override
    public List<UserRepresentation> findAllUsers() {
        return KeyCloakProvider.getRealmResouce().users().list();
    }

    @Override
    public List<UserRepresentation> searchUserByUsername(String username) {
        return KeyCloakProvider.getRealmResouce().users().search(username, true);
    }

    @Override
    public String createUser(@NotNull UserDto userDto) {
        int status  = 0;
        UsersResource usersResource = KeyCloakProvider.getUserResource();

        Response response = createNewUser(userDto, usersResource);
        status = response.getStatus();

        if (status == 201){
            String path = response.getLocation().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);
            setPasswordUser(userDto, usersResource, userId);

            assignRoleToUser(userDto, userId);
            return "User created successfully, User ID: " + userId;

        } else if (status == 409) {
            log.error("User already exists");
            return "User already exists";

        } else {
            log.error("Error creating user");
            return "Error creating user";

        }

    }

    private void assignRoleToUser(UserDto userDto, String userId) {

        RealmResource realmResource = KeyCloakProvider.getRealmResouce();
        List<RoleRepresentation> roleRepresentations = null;

        if (userDto.roles() == null || userDto.roles().isEmpty()) {
            roleRepresentations = List.of(realmResource.roles().get(KC_USER_ROLE).toRepresentation());
        } else {
            List<String> availableRoles = realmResource.roles().list().stream()
                    .map(RoleRepresentation::getName)
                    .toList();

            List<String> nonExistentRoles = userDto.roles().stream()
                    .filter(role -> !availableRoles.contains(role))
                    .toList();

            if (!nonExistentRoles.isEmpty()) {
                log.warn("The following roles don't exist in Keycloak: {}", nonExistentRoles);
            }

            roleRepresentations = realmResource.roles()
                    .list()
                    .stream()
                    .filter(role -> userDto.roles()
                            .stream()
                            .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getName())))
                    .toList();

            if (roleRepresentations.isEmpty()) {
                log.warn("No valid roles found. Assigning default role.");
                roleRepresentations = List.of(realmResource.roles().get(KC_USER_ROLE).toRepresentation());
            }
        }

        realmResource.users().get(userId)
                .roles()
                .realmLevel()
                .add(roleRepresentations);

    }



    private void setPasswordUser(UserDto userDto, UsersResource usersResource, String userId) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setType(OAuth2Constants.PASSWORD);
        credentialRepresentation.setValue(userDto.password());

        usersResource.get(userId).resetPassword(credentialRepresentation);

    }

    private Response createNewUser(UserDto userDto,  UsersResource usersResource){
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.username());
        userRepresentation.setEmail(userDto.email());
        userRepresentation.setEmailVerified(true); //lo ideal es enviar un email de verificacion
        userRepresentation.setFirstName(userDto.firstName());
        userRepresentation.setLastName(userDto.lastName());
        userRepresentation.setEnabled(true);

        Response response = usersResource.create(userRepresentation);
        return response;
    }

    public String createUser2(@NotNull UserDto userDto) {
        UsersResource usersResource = KeyCloakProvider.getUserResource();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setEmailVerified(true);
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setEnabled(true);

        try {
            Response response = usersResource.create(user);
            String body = response.readEntity(String.class);
            log.error("Response status: {}, body: {}", response.getStatus(), body);
            response.close();


            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                // Set password
                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setTemporary(false);
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(userDto.password());

                usersResource.get(userId).resetPassword(passwordCred);

                // Set default role
                RoleRepresentation userRole = KeyCloakProvider.getRealmResouce()
                        .roles()
                        .get("user")
                        .toRepresentation();

                usersResource.get(userId)
                        .roles()
                        .realmLevel()
                        .add(List.of(userRole));

                return "User created with ID: " + userId;
            }

            throw new RuntimeException("Failed to create user. Status: " + response.getStatus() + " Body: " + body);

        } catch (Exception e) {
            throw new RuntimeException("Error creating user: " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(String userId) {
        KeyCloakProvider.getUserResource().get(userId).remove();
    }

    @Override
    public void updateUser(String userId, @NotNull UserDto userDto) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(OAuth2Constants.PASSWORD);
        credential.setValue(userDto.password());

        UserRepresentation userRepresentation =new UserRepresentation();
        userRepresentation.setFirstName(userDto.firstName());
        userRepresentation.setLastName(userDto.lastName());
        userRepresentation.setEmail(userDto.email());
        userRepresentation.setEmailVerified(true);
        userRepresentation.setUsername(userDto.username());
        userRepresentation.setEnabled(true);
        userRepresentation.setCredentials(Collections.singletonList(credential));

        UserResource userResource = KeyCloakProvider.getUserResource().get(userId);
        userResource.update(userRepresentation);

    }
}
