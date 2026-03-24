package com.example.todo.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AuditLog {
    private Long id;
    private String eventType;
    private String detail;
    private String actor;
    private LocalDateTime createdAt;
}
