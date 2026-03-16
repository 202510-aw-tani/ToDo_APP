package com.example.todo.mapper;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.AppUser;

public interface UserMapper {
    AppUser findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int insert(AppUser user);
}
