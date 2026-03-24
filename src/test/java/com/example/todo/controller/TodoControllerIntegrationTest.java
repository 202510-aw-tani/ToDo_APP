package com.example.todo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.todo.model.Category;
import com.example.todo.service.CategoryService;
import com.example.todo.service.TodoAttachmentService;
import com.example.todo.service.TodoService;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private TodoAttachmentService todoAttachmentService;

    @Test
    @DisplayName("GET /todo/new: 新規登録画面を表示できる")
    @WithMockUser(username = "user", roles = "USER")
    void newTodo_shouldReturnFormView() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/todo/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/form"));
    }

    @Test
    @DisplayName("POST /todo/confirm: 入力内容確認画面を表示できる")
    @WithMockUser(username = "user", roles = "USER")
    void confirm_shouldReturnConfirmView() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Work");
        when(categoryService.findById(1L)).thenReturn(category);

        mockMvc.perform(post("/todo/confirm")
                        .with(csrf())
                        .param("title", "Write tests")
                        .param("priority", "HIGH")
                        .param("categoryId", "1")
                        .param("deadline", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/confirm"))
                .andExpect(model().attributeExists("title", "priority", "category", "deadline"));
    }
}
