package com.example.todo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.example.todo.audit.Auditable;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.mapper.UserMapper;
import com.example.todo.model.AppUser;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, TodoMapper todoMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.todoMapper = todoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAll() {
        return userMapper.findAll();
    }

    public AppUser findById(Long id) {
        return userMapper.findById(id);
    }

    public boolean updateRoleAndEnabled(Long id, String role, boolean enabled) {
        AppUser user = userMapper.findById(id);
        if (user == null) {
            return false;
        }
        String normalizedRole = normalizeRole(role);
        return userMapper.updateRoleAndEnabled(id, normalizedRole, enabled) > 0;
    }

    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userMapper.countByUsername(username.trim()) > 0;
    }

    public AppUser register(String username, String rawPassword) {
        String normalizedUsername = username.trim();
        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedUsername + "@example.local");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    @Auditable(action = "USER_UPDATE_ADMIN", entityType = "USER", entityIdArgIndex = 0, beforeMethod = "findById", afterMethod = "findById")
    public boolean updateByAdmin(Long id, String role, boolean enabled, String newPassword) {
        AppUser user = userMapper.findById(id);
        if (user == null) {
            return false;
        }
        String normalizedRole = normalizeRole(role);
        String encodedPassword = StringUtils.hasText(newPassword) ? passwordEncoder.encode(newPassword) : null;
        return userMapper.updateByAdmin(id, normalizedRole, enabled, encodedPassword) > 0;
    }

    @Transactional
    @Auditable(action = "USER_DELETE_ADMIN", entityType = "USER", entityIdArgIndex = 0, beforeMethod = "findById")
    public boolean deleteUserAndTodos(Long id) {
        AppUser user = userMapper.findById(id);
        if (user == null) {
            return false;
        }
        todoMapper.deleteByUserId(id);
        return userMapper.deleteById(id) > 0;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }
        String upper = role.trim().toUpperCase();
        if (!upper.startsWith("ROLE_")) {
            upper = "ROLE_" + upper;
        }
        if ("ROLE_ADMIN".equals(upper)) {
            return "ROLE_ADMIN";
        }
        return "ROLE_USER";
    }
}
