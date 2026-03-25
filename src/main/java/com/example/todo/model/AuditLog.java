package com.example.todo.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AuditLog {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private Long userId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdAt;
}
