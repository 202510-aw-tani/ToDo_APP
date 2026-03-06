package com.example.todo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public void create(String title, Priority priority, Long categoryId) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setCategoryId(categoryId);
        todoMapper.insert(todo);
    }

    public java.util.List<Todo> findAll() {
        return todoMapper.findAll();
    }

    public Page<Todo> findPage(Pageable pageable, boolean sortByPriority, Long categoryId) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        java.util.List<Todo> content = todoMapper.findPage(limit, offset, sortByPriority, categoryId);
        long total = todoMapper.countAll(categoryId);
        return new PageImpl<>(content, pageable, total);
    }

    public boolean deleteById(Long id) {
        return todoMapper.deleteById(id) > 0;
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public boolean update(Long id, String title, Priority priority, Long categoryId) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setCategoryId(categoryId);
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
