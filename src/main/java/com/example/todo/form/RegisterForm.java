package com.example.todo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;
}

