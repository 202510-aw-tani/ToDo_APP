package com.example.todo.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TodoHistory {
    private Long id;
    private Long todoId;
    private String action;
    private String detail;
    private String actor;
    private LocalDateTime createdAt;
}
