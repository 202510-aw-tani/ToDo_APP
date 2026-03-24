package com.example.todo.controller;

import jakarta.validation.Valid;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.todo.form.RegisterForm;
import com.example.todo.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/todo";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/todo";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/todo";
        }

        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registerForm", "confirmPassword", "Passwords do not match."));
        }

        if (!bindingResult.hasFieldErrors("username") && userService.existsByUsername(form.getUsername())) {
            bindingResult.addError(new FieldError("registerForm", "username", "Username is already in use."));
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        userService.register(form.getUsername(), form.getPassword());
        redirectAttributes.addFlashAttribute("registeredMessage", "Registration completed. Please sign in.");
        return "redirect:/login";
    }
}
