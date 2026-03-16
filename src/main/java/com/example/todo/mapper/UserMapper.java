package com.example.todo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.AppUser;

public interface UserMapper {
    AppUser findByUsername(@Param("username") String username);

    List<AppUser> findAll();

    AppUser findById(@Param("id") Long id);

    int countByUsername(@Param("username") String username);

    int insert(AppUser user);

    int updateRoleAndEnabled(@Param("id") Long id,
            @Param("role") String role,
            @Param("enabled") boolean enabled);
}
