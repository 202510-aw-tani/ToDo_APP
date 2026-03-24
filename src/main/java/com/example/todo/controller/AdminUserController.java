package com.example.todo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.example.todo.security.LoginUserPrincipal;
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
            @RequestParam(name = "newPassword", required = false) String newPassword,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            RedirectAttributes redirectAttributes) {
        boolean passwordResetRequested = hasText(newPassword) || hasText(confirmPassword);
        if (passwordResetRequested && !safe(newPassword).equals(safe(confirmPassword))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password confirmation does not match.");
            return "redirect:/admin/users/" + id + "/edit";
        }

        boolean updated = userService.updateByAdmin(id, role, enabled, safe(newPassword));
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update user.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        if (loginUser != null && id.equals(loginUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own account.");
            return "redirect:/admin/users";
        }

        boolean deleted = userService.deleteUserAndTodos(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "User and related todos were deleted.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete user.");
        }
        return "redirect:/admin/users";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

