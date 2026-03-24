package com.example.todo.service;

import java.time.LocalDate;
import java.util.List;

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

    public Page<Todo> findPage(Pageable pageable, boolean sortByPriority, boolean sortByDeadline, Long categoryId,
            Long userId, boolean admin) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        List<Todo> content;
        long total;

        if (admin) {
            content = todoMapper.findPageForAdmin(limit, offset, sortByPriority, sortByDeadline, categoryId);
            total = todoMapper.countAllForAdmin(categoryId);
        } else {
            content = todoMapper.findPage(limit, offset, sortByPriority, sortByDeadline, categoryId, userId);
            total = todoMapper.countAll(categoryId, userId);
        }
        return new PageImpl<>(content, pageable, total);
    }

    public List<Todo> findAllForExport(Long userId, boolean admin) {
        return admin ? todoMapper.findAll() : todoMapper.findAllByUserId(userId);
    }

    public List<Todo> findAllForApi() {
        return todoMapper.findAll();
    }

    public Todo findByIdForApi(Long id) {
        return todoMapper.findById(id);
    }

    public Todo createForApi(String title, boolean completed, Priority priority, Long categoryId, LocalDate deadline) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(null);
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        todo.setCreatedBy("api");
        todo.setCreatedAt(LocalDate.now());
        todoMapper.insert(todo);
        return todoMapper.findById(todo.getId());
    }

    public Todo updateForApi(Long id, String title, boolean completed, Priority priority, Long categoryId,
            LocalDate deadline) {
        Todo current = todoMapper.findById(id);
        if (current == null) {
            return null;
        }
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(current.getUserId());
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        int updated = todoMapper.updateById(todo);
        if (updated == 0) {
            return null;
        }
        return todoMapper.findById(id);
    }

    public boolean deleteForApi(Long id) {
        return todoMapper.deleteById(id) > 0;
    }

    public Todo findByIdForAccess(Long id, Long userId, boolean admin) {
        return admin ? todoMapper.findById(id) : todoMapper.findByIdAndUserId(id, userId);
    }

    public boolean deleteById(Long id, Long userId, boolean admin) {
        return admin ? todoMapper.deleteById(id) > 0 : todoMapper.deleteByIdAndUserId(id, userId) > 0;
    }

    public int deleteByIds(List<Integer> ids, Long userId, boolean admin) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return admin ? todoMapper.deleteByIds(ids) : todoMapper.deleteByIdsAndUserId(ids, userId);
    }

    public boolean update(Long id, String title, Priority priority, Long categoryId, LocalDate deadline, Long userId,
            boolean admin) {
        Todo current = findByIdForAccess(id, userId, admin);
        if (current == null) {
            return false;
        }

        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(current.getCompleted());
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(current.getUserId());
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);

        return admin ? todoMapper.updateById(todo) > 0 : todoMapper.updateByIdAndUserId(todo) > 0;
    }

    public boolean toggleCompleted(Long id, Long userId, boolean admin) {
        Todo todo = findByIdForAccess(id, userId, admin);
        if (todo == null) {
            return false;
        }
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        return admin ? todoMapper.updateById(todo) > 0 : todoMapper.updateByIdAndUserId(todo) > 0;
    }

    public boolean existsById(Long id) {
        return todoMapper.existsById(id) > 0;
    }

    public boolean isOwner(Long id, Long userId) {
        return todoMapper.countByIdAndUserId(id, userId) > 0;
    }
}
