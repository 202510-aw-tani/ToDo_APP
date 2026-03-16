package com.example.todo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.todo.model.AppUser;
import com.example.todo.service.UserService;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        AppUser user = userService.findById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("user", user);
        return "admin/user-edit";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
            @RequestParam("role") String role,
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            RedirectAttributes redirectAttributes) {
        boolean updated = userService.updateRoleAndEnabled(id, role, enabled);
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "ユーザー情報を更新しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "ユーザー情報の更新に失敗しました。");
        }
        return "redirect:/admin/users";
    }
}
