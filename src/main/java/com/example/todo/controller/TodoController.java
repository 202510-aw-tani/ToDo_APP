package com.example.todo.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.todo.model.Category;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.service.CategoryService;
import com.example.todo.service.TodoService;

@Controller
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;
    private final CategoryService categoryService;

    public TodoController(TodoService todoService, CategoryService categoryService) {
        this.todoService = todoService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            Model model) {
        int size = 10;
        boolean sortByPriority = "priority".equalsIgnoreCase(sort);
        boolean sortByDeadline = "deadline".equalsIgnoreCase(sort);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Todo> todoPage = todoService.findPage(pageable, sortByPriority, sortByDeadline, categoryId);

        long total = todoPage.getTotalElements();
        int currentPage = todoPage.getNumber();
        long start = total == 0 ? 0 : (long) currentPage * size + 1;
        long end = Math.min((long) (currentPage + 1) * size, total);

        model.addAttribute("todoPage", todoPage);
        model.addAttribute("pageNumbers", java.util.stream.IntStream.range(0, todoPage.getTotalPages()).toArray());
        model.addAttribute("totalCount", total);
        model.addAttribute("rangeStart", start);
        model.addAttribute("rangeEnd", end);
        model.addAttribute("sort", sortByDeadline ? "deadline" : (sortByPriority ? "priority" : "id"));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newTodo(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "todo/form";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam("title") String title,
            @RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            Model model) {
        model.addAttribute("title", title);
        model.addAttribute("priority", priority);
        Category category = categoryId != null ? categoryService.findById(categoryId) : null;
        model.addAttribute("category", category);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("deadline", deadline);
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam("title") String title,
            @RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline) {
        todoService.create(title, priority, categoryId, deadline);
        return "redirect:/todo";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = todoService.deleteById(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "ToDoの削除に失敗しました。");
        }
        return "redirect:/todo";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(name = "ids", required = false) List<Integer> ids,
            RedirectAttributes redirectAttributes) {
        int deleted = todoService.deleteByIds(ids);
        if (deleted > 0) {
            redirectAttributes.addFlashAttribute("successMessage", deleted + "件のToDoを削除しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "削除対象のToDoが選択されていません。");
        }
        return "redirect:/todo";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = buildCsv(todoService.findAll());
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, data, 0, bom.length);
        System.arraycopy(body, 0, data, bom.length, body.length);

        String filename = "todo_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("todo", todoService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        return "todo/edit";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            RedirectAttributes redirectAttributes) {
        boolean updated = todoService.update(id, title, priority, categoryId, deadline);
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを更新しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "ToDoの更新に失敗しました。");
        }
        return "redirect:/todo";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/todo";
    }

    private String buildCsv(List<Todo> todos) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,タイトル,作成者,状態,作成日").append("\r\n");
        for (Todo todo : todos) {
            String status = Boolean.TRUE.equals(todo.getCompleted()) ? "完了" : "未完了";
            String createdAt = todo.getCreatedAt() != null ? todo.getCreatedAt().toString() : "";
            sb.append(escapeCsv(String.valueOf(todo.getId()))).append(',')
                    .append(escapeCsv(todo.getTitle())).append(',')
                    .append(escapeCsv(todo.getCreatedBy())).append(',')
                    .append(escapeCsv(status)).append(',')
                    .append(escapeCsv(createdAt)).append("\r\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }
}
