package com.example.todo.security;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.todo.service.TodoService;

@Service("todoSecurityService")
public class TodoSecurityService {

    private final TodoService todoService;

    public TodoSecurityService(TodoService todoService) {
        this.todoService = todoService;
    }

    public boolean isOwner(Long id, Object principal) {
        if (!(principal instanceof LoginUserPrincipal loginUser)) {
            return false;
        }
        return todoService.isOwner(id, loginUser.getId());
    }

    public boolean areAllOwned(List<Integer> ids, Object principal) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        if (!(principal instanceof LoginUserPrincipal loginUser)) {
            return false;
        }
        return ids.stream().allMatch(id -> todoService.isOwner(id.longValue(), loginUser.getId()));
    }
}
