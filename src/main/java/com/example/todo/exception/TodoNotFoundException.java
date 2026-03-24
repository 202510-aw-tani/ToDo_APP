package com.example.todo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("Todo not found. id=" + id);
    }
}

