package com.example.todo.model;

import lombok.Data;

@Data
public class AppUser {
    private Long id;
    private String username;
    private String password;
    private String role;
    private Boolean enabled;
}
