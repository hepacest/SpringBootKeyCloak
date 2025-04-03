package com.api.rest.service;

import com.api.rest.controller.dto.UserDto;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface IKeycloakService {

    List<UserRepresentation> findAllUsers();
    List<UserRepresentation> searchUserByUsername (String username);
    String createUser(UserDto userDto);
    void deleteUser(String userId);
    void updateUser(String userId, UserDto userDto);


}
