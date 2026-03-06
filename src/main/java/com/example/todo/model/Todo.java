package com.example.todo.model;

import lombok.Data;

@Data
public class Todo {
    private Long id;
    private String title;
    private Boolean completed;
    private Priority priority;
    private Long categoryId;
    private Category category;
}
