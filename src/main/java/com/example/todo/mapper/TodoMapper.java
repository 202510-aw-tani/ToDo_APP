package com.example.todo.mapper;

import java.util.List;

import com.example.todo.model.Todo;

public interface TodoMapper {
    List<Todo> findAll();

    int insert(Todo todo);
}
