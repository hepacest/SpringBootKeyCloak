package com.api.rest.controller;


import com.api.rest.controller.dto.UserDto;
import com.api.rest.service.IKeycloakService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/keycloak/user")
@PreAuthorize("hasRole('admin_client_role')") // viene de keycloak
public class KeycloakController {

    private IKeycloakService keycloakService;

    public KeycloakController(IKeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> findAllUsers() {
        return ResponseEntity.ok(keycloakService.findAllUsers());
    }


    @GetMapping("/search/{username}")
    public ResponseEntity<?> findUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(keycloakService.searchUserByUsername(username));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto) throws URISyntaxException {
        String response = keycloakService.createUser(userDto);
        return ResponseEntity.created(new URI("/keycloak/user/create")).body(response);

    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<?> uddateUser(@PathVariable String userId, @RequestBody UserDto userDto)  {
       keycloakService.updateUser(userId, userDto);
        return ResponseEntity.ok("User updated successfully");

    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        keycloakService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

}
