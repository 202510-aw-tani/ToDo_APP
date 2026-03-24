package com.example.todo.mapper;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.TodoHistory;

public interface TodoHistoryMapper {
    int insert(TodoHistory history);

    int deleteByTodoId(@Param("todoId") Long todoId);
}
