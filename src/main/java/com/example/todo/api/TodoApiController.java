package com.example.todo.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Todo not found.", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched todo.", todo));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Todo>> create(@Valid @RequestBody TodoApiRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<Todo>> badRequest = validateRequest(bindingResult);
        if (badRequest != null) {
            return badRequest;
        }

        Priority priority = parsePriority(request.getPriority());
        if (priority == null && hasText(request.getPriority())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Invalid priority. Use HIGH, MEDIUM, or LOW.", null));
        }

        Todo created = todoService.createForApi(
                request.getTitle().trim(),
                Boolean.TRUE.equals(request.getCompleted()),
                priority,
                request.getCategoryId(),
                request.getDeadline());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Todo created.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Todo>> update(@PathVariable("id") Long id,
            @Valid @RequestBody TodoApiRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<Todo>> badRequest = validateRequest(bindingResult);
        if (badRequest != null) {
            return badRequest;
        }

        Priority priority = parsePriority(request.getPriority());
        if (priority == null && hasText(request.getPriority())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Invalid priority. Use HIGH, MEDIUM, or LOW.", null));
        }

        Todo updated = todoService.updateForApi(
                id,
                request.getTitle().trim(),
                Boolean.TRUE.equals(request.getCompleted()),
                priority,
                request.getCategoryId(),
                request.getDeadline());

        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Todo not found.", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Todo updated.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        boolean deleted = todoService.deleteForApi(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Todo not found.", null));
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Invalid JSON request.", null));
    }

    private ResponseEntity<ApiResponse<Todo>> validateRequest(BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            return null;
        }
        FieldError first = bindingResult.getFieldError();
        String message = first != null ? first.getDefaultMessage() : "Validation failed.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null));
    }

    private Priority parsePriority(String raw) {
        if (!hasText(raw)) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
