package com.example.todo.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.todo.mapper.TodoMapper;
import com.example.todo.mapper.UserMapper;
import com.example.todo.model.AppUser;

@Component
public class UserDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(UserMapper userMapper, TodoMapper todoMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.todoMapper = todoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        AppUser admin = ensureUser("admin", "admin123", "ROLE_ADMIN");
        ensureUser("user", "user123", "ROLE_USER");

        if (admin != null && !Boolean.TRUE.equals(admin.getEnabled())) {
            userMapper.updateRoleAndEnabled(admin.getId(), "ROLE_ADMIN", true);
            admin.setEnabled(true);
            admin.setRole("ROLE_ADMIN");
        } else if (admin != null && !"ROLE_ADMIN".equals(admin.getRole())) {
            userMapper.updateRoleAndEnabled(admin.getId(), "ROLE_ADMIN", true);
            admin.setRole("ROLE_ADMIN");
        }

        if (admin != null) {
            todoMapper.assignUnownedTodosToUser(admin.getId());
        }
    }

    private AppUser ensureUser(String username, String rawPassword, String role) {
        AppUser existing = userMapper.findByUsername(username);
        if (existing != null) {
            return existing;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        userMapper.insert(user);
        return userMapper.findByUsername(username);
    }
}
