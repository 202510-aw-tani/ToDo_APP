package com.example.todo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.Todo;

public interface TodoMapper {
    List<Todo> findAll();

    List<Todo> findPage(@Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortByPriority") boolean sortByPriority);

    long countAll();

    int insert(Todo todo);

    int deleteById(Long id);

    Todo findById(Long id);

    int update(Todo todo);
}
