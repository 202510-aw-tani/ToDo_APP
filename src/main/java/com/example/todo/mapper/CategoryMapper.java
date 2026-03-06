package com.example.todo.mapper;

import java.util.List;

import com.example.todo.model.Category;

public interface CategoryMapper {
    List<Category> findAll();

    Category findById(Long id);
}
