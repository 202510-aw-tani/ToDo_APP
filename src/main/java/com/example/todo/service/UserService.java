package com.example.todo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.todo.mapper.UserMapper;
import com.example.todo.model.AppUser;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
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
