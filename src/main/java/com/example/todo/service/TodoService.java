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

    public java.util.List<Todo> findAll() {
        return todoMapper.findAll();
    }

    public boolean deleteById(Long id) {
        return todoMapper.deleteById(id) > 0;
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public boolean update(Long id, String title) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(false);
        return todoMapper.update(todo) > 0;
    }

    public boolean toggleCompleted(Long id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            return false;
        }
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        return todoMapper.update(todo) > 0;
    }
}
