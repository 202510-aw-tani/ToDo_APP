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
        AppUser admin = userMapper.findByUsername("admin");
        if (admin == null) {
            AppUser newAdmin = new AppUser();
            newAdmin.setUsername("admin");
            newAdmin.setPassword(passwordEncoder.encode("admin123"));
            newAdmin.setRole("ROLE_USER");
            userMapper.insert(newAdmin);
            admin = userMapper.findByUsername("admin");
        }

        if (admin != null) {
            todoMapper.assignUnownedTodosToUser(admin.getId());
        }
    }
}
