package com.example.todo.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Todo {
    private Long id;
    private String title;
    private Boolean completed;
    private Priority priority;
    private Long categoryId;
    private Category category;
    private LocalDate deadline;

    public boolean isOverdue() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    public boolean isNearDeadline() {
        if (deadline == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !deadline.isBefore(today) && !deadline.isAfter(today.plusDays(3));
    }

    public Long getDaysUntilDeadline() {
        if (deadline == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }
}
