package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.todo.service.TodoService;

@Controller
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size);
        Page<com.example.todo.model.Todo> todoPage = todoService.findPage(pageable);
        long total = todoPage.getTotalElements();
        int currentPage = todoPage.getNumber();
        long start = total == 0 ? 0 : (long) currentPage * size + 1;
        long end = Math.min((long) (currentPage + 1) * size, total);

        model.addAttribute("todoPage", todoPage);
        model.addAttribute("pageNumbers", java.util.stream.IntStream.range(0, todoPage.getTotalPages()).toArray());
        model.addAttribute("totalCount", total);
        model.addAttribute("rangeStart", start);
        model.addAttribute("rangeEnd", end);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newTodo() {
        return "todo/form";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam("title") String title, Model model) {
        model.addAttribute("title", title);
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam("title") String title) {
        todoService.create(title);
        return "redirect:/todo";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = todoService.deleteById(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
        }
        return "redirect:/todo";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("todo", todoService.findById(id));
        return "todo/edit";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes) {
        boolean updated = todoService.update(id, title);
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "更新に失敗しました");
        }
        return "redirect:/todo";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/todo";
    }
}
