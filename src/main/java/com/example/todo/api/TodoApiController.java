package com.example.todo.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.todo.exception.BusinessException;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.service.TodoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/todo")
@CrossOrigin(origins = "*")
public class TodoApiController {

    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Todo>>> findAll() {
        List<Todo> todos = todoService.findAllForApi();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched todo list.", todos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Todo>> findById(@PathVariable("id") Long id) {
        Todo todo = todoService.findByIdForApi(id);
        if (todo == null) {
            throw new TodoNotFoundException(id);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched todo.", todo));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Todo>> create(@Valid @RequestBody TodoApiRequest request) {
        Priority priority = parsePriority(request.getPriority());
        Todo created = todoService.createForApi(
                request.getTitle().trim(),
                Boolean.TRUE.equals(request.getCompleted()),
                priority,
                request.getCategoryId(),
                request.getDeadline());
        return ResponseEntity.status(201)
                .body(new ApiResponse<>(true, "Todo created.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Todo>> update(@PathVariable("id") Long id,
            @Valid @RequestBody TodoApiRequest request) {
        Priority priority = parsePriority(request.getPriority());

        Todo updated = todoService.updateForApi(
                id,
                request.getTitle().trim(),
                Boolean.TRUE.equals(request.getCompleted()),
                priority,
                request.getCategoryId(),
                request.getDeadline());
        if (updated == null) {
            throw new TodoNotFoundException(id);
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Todo updated.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        boolean deleted = todoService.deleteForApi(id);
        if (!deleted) {
            throw new TodoNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

    private Priority parsePriority(String raw) {
        if (!hasText(raw)) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid priority. Use HIGH, MEDIUM, or LOW.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
