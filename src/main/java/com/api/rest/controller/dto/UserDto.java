package com.api.rest.controller.dto;

import java.util.Set;


public record UserDto(String username, String password, String email, String firstName, String lastName,
                      Set<String> roles) {
}
