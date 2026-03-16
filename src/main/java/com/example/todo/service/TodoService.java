package com.example.todo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public void create(String title, Priority priority, Long categoryId, LocalDate deadline, Long userId,
            String username) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(userId);
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        todo.setCreatedBy(username);
        todo.setCreatedAt(LocalDate.now());
        todoMapper.insert(todo);
    }

    public java.util.List<Todo> findAllByUserId(Long userId) {
        return todoMapper.findAllByUserId(userId);
    }

    public Page<Todo> findPage(Pageable pageable, boolean sortByPriority, boolean sortByDeadline, Long categoryId,
            Long userId) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        java.util.List<Todo> content = todoMapper.findPage(limit, offset, sortByPriority, sortByDeadline, categoryId,
                userId);
        long total = todoMapper.countAll(categoryId, userId);
        return new PageImpl<>(content, pageable, total);
    }

    public boolean deleteByIdAndUserId(Long id, Long userId) {
        return todoMapper.deleteByIdAndUserId(id, userId) > 0;
    }

    public int deleteByIdsAndUserId(java.util.List<Integer> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return todoMapper.deleteByIdsAndUserId(ids, userId);
    }

    public Todo findByIdAndUserId(Long id, Long userId) {
        return todoMapper.findByIdAndUserId(id, userId);
    }

    public boolean update(Long id, String title, Priority priority, Long categoryId, LocalDate deadline, Long userId) {
        Todo current = todoMapper.findByIdAndUserId(id, userId);
        if (current == null) {
            return false;
        }

        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(current.getCompleted());
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(userId);
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        return todoMapper.updateByIdAndUserId(todo) > 0;
    }

    public boolean toggleCompleted(Long id, Long userId) {
        Todo todo = todoMapper.findByIdAndUserId(id, userId);
        if (todo == null) {
            return false;
        }
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        return todoMapper.updateByIdAndUserId(todo) > 0;
    }

    public boolean existsById(Long id) {
        return todoMapper.existsById(id) > 0;
    }

    public boolean isOwner(Long id, Long userId) {
        return todoMapper.countByIdAndUserId(id, userId) > 0;
    }
}
