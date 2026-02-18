package com.example.todo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/todo")
public class TodoController {

    @GetMapping
    public String list(Model model) {
        List<String> todos = List.of(
                "買い物に行く",
                "Spring Bootの勉強をする",
                "メールを返信する"
        );
        model.addAttribute("todos", todos);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newTodo() {
        return "todo/new";
    }

    @GetMapping("/confirm")
    public String confirm() {
        return "todo/confirm";
    }
}
