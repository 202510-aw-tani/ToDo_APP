package com.example.todo.service;

import org.springframework.stereotype.Service;

import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Todo;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public void create(String title) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        todoMapper.insert(todo);
    }
}
