package com.example.todo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.todo.mapper.CategoryMapper;
import com.example.todo.model.Category;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    public Category findById(Long id) {
        return categoryMapper.findById(id);
    }
}
