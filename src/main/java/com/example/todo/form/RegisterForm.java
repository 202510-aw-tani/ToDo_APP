package com.example.todo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank(message = "{validation.username.required}")
    @Size(max = 100, message = "{validation.username.max}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 72, message = "{validation.password.size}")
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;
}

