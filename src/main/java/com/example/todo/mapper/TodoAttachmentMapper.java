package com.example.todo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.TodoAttachment;

public interface TodoAttachmentMapper {
    int insert(TodoAttachment attachment);

    TodoAttachment findById(@Param("id") Long id);

    List<TodoAttachment> findByTodoId(@Param("todoId") Long todoId);

    int deleteById(@Param("id") Long id);

    int deleteByTodoId(@Param("todoId") Long todoId);
}
