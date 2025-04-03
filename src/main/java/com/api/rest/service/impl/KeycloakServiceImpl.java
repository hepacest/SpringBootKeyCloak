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
        int status = 0;
        UsersResource usersResource = KeyCloakProvider.getUserResource();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setEmailVerified(true);
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setEnabled(true);


        Response response = usersResource.create(user);
        status = response.getStatus();

        if (status == 201) {
            String path = response.getLocation().getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setTemporary(false);
            credential.setType(OAuth2Constants.PASSWORD);
            credential.setValue(userDto.password());
            usersResource.get(userId).resetPassword(credential);

            RealmResource realmResource = KeyCloakProvider.getRealmResouce();
            List<RoleRepresentation> roleRepresentations = null;
            if (userDto.roles() == null || userDto.roles().isEmpty()) {
                roleRepresentations = List.of(realmResource.roles().get("user").toRepresentation());
            } else {
                roleRepresentations = realmResource.roles()
                        .list()
                        .stream()
                        .filter(role -> userDto.roles()
                                .stream()
                                .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getName()))
                        )
                        .toList();
            }

            realmResource.users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(roleRepresentations);

            return "User with ID " + user.getId() + " has been added successfully";
        }else if (status == 409) {
            log.error("User with username {} already exists", user.getUsername());
            return "User with username " + user.getUsername() + " already exists";
        }else{
            log.error("Failed to create user with username {}", user.getUsername());
            return "Failed to create user with username " + user.getUsername();
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
