package com.example.todo.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.model.Category;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.model.TodoAttachment;
import com.example.todo.security.LoginUserPrincipal;
import com.example.todo.service.CategoryService;
import com.example.todo.service.TodoAttachmentService;
import com.example.todo.service.TodoService;

/**
 * ToDo画面のMVCコントローラーです。
 *
 * <p>{@code /todo} 配下の一覧、登録、編集、削除、CSV出力、
 * 添付ファイル操作を提供します。</p>
 *
 * @author academia
 * @version 1.0
 * @since 1.0
 * @see TodoService
 */
@Controller
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;
    private final CategoryService categoryService;
    private final TodoAttachmentService todoAttachmentService;
    private final MessageSource messageSource;

    /**
     * コンストラクタです。
     *
     * @param todoService ToDoサービス
     * @param categoryService カテゴリサービス
     * @param todoAttachmentService 添付ファイルサービス
     * @param messageSource メッセージソース
     */
    public TodoController(TodoService todoService, CategoryService categoryService,
            TodoAttachmentService todoAttachmentService, MessageSource messageSource) {
        this.todoService = todoService;
        this.categoryService = categoryService;
        this.todoAttachmentService = todoAttachmentService;
        this.messageSource = messageSource;
    }

    /**
     * HTTP GET {@code /todo}。
     *
     * <p>ToDo一覧をページング付きで表示します。</p>
     *
     * @param page ページ番号
     * @param sort ソート種別
     * @param categoryId カテゴリID
     * @param loginUser ログインユーザー
     * @param model 画面モデル
     * @return 一覧テンプレート名
     */
    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            Model model) {
        int size = 10;
        boolean sortByPriority = "priority".equalsIgnoreCase(sort);
        boolean sortByDeadline = "deadline".equalsIgnoreCase(sort);
        boolean admin = loginUser.isAdmin();

        PageRequest pageable = PageRequest.of(page, size);
        Page<Todo> todoPage = todoService.findPage(pageable, sortByPriority, sortByDeadline, categoryId,
                loginUser.getId(), admin);

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
        model.addAttribute("isAdmin", admin);
        return "todo/list";
    }

    /**
     * HTTP GET {@code /todo/new}。
     *
     * <p>ToDo新規作成フォームを表示します。</p>
     *
     * @param model 画面モデル
     * @return フォームテンプレート名
     */
    @GetMapping("/new")
    public String newTodo(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "todo/form";
    }

    /**
     * HTTP POST {@code /todo/confirm}。
     *
     * <p>入力内容確認画面を表示します。</p>
     *
     * @param title タイトル
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @param model 画面モデル
     * @return 確認テンプレート名
     */
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

    /**
     * HTTP POST {@code /todo/complete}。
     *
     * <p>ToDoを登録して一覧画面へリダイレクトします。</p>
     *
     * @param title タイトル
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @param loginUser ログインユーザー
     * @return リダイレクト先
     */
    @PostMapping("/complete")
    public String complete(@RequestParam("title") String title,
            @RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @AuthenticationPrincipal LoginUserPrincipal loginUser) {
        todoService.create(title, priority, categoryId, deadline, loginUser.getId(), loginUser.getUsername());
        return "redirect:/todo";
    }

    /**
     * HTTP POST {@code /todo/{id}/delete}。
     *
     * <p>指定ToDoを削除します。</p>
     *
     * @param id ToDo ID
     * @param loginUser ログインユーザー
     * @param redirectAttributes リダイレクト属性
     * @return リダイレクト先
     * @throws TodoNotFoundException 対象ToDoが存在しない場合
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.isOwner(#id, principal)")
    public String delete(@PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        boolean deleted = todoService.deleteById(id, loginUser.getId(), loginUser.isAdmin());
        if (!deleted) {
            throw new TodoNotFoundException(id);
        }
        redirectAttributes.addFlashAttribute("successMessage", message("todo.message.deleted"));
        return "redirect:/todo";
    }

    /**
     * HTTP POST {@code /todo/bulk-delete}。
     *
     * <p>選択されたToDoを一括削除します。</p>
     *
     * @param ids 削除対象ID一覧
     * @param loginUser ログインユーザー
     * @param redirectAttributes リダイレクト属性
     * @return リダイレクト先
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.areAllOwned(#ids, principal)")
    public String bulkDelete(@RequestParam(name = "ids", required = false) List<Integer> ids,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        int deleted = todoService.deleteByIds(ids, loginUser.getId(), loginUser.isAdmin());
        if (deleted > 0) {
            redirectAttributes.addFlashAttribute("successMessage", message("todo.message.bulkDeleted", deleted));
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", message("todo.message.bulkDeleteEmpty"));
        }
        return "redirect:/todo";
    }

    /**
     * HTTP GET {@code /todo/export}。
     *
     * <p>ToDo一覧をCSVとしてダウンロードします。</p>
     *
     * @param loginUser ログインユーザー
     * @return CSVレスポンス
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal LoginUserPrincipal loginUser) {
        String csv = buildCsv(todoService.findAllForExport(loginUser.getId(), loginUser.isAdmin()));
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

    /**
     * HTTP GET {@code /todo/{id}/edit}。
     *
     * <p>ToDo編集画面を表示します。</p>
     *
     * @param id ToDo ID
     * @param loginUser ログインユーザー
     * @param model 画面モデル
     * @return 編集テンプレート名
     * @throws TodoNotFoundException 対象ToDoが存在しない場合
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.isOwner(#id, principal)")
    public String edit(@PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            Model model) {
        Todo todo = todoService.findByIdForAccess(id, loginUser.getId(), loginUser.isAdmin());
        if (todo == null) {
            throw new TodoNotFoundException(id);
        }
        model.addAttribute("todo", todo);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("attachments", todoAttachmentService.findByTodoId(id));
        return "todo/edit";
    }

    /**
     * HTTP POST {@code /todo/{id}/attachments}。
     *
     * <p>添付ファイルをアップロードします。</p>
     *
     * @param id ToDo ID
     * @param file アップロードファイル
     * @param loginUser ログインユーザー
     * @param redirectAttributes リダイレクト属性
     * @return リダイレクト先
     * @throws TodoNotFoundException 対象ToDoが存在しない場合
     */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.isOwner(#id, principal)")
    public String uploadAttachment(@PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findByIdForAccess(id, loginUser.getId(), loginUser.isAdmin());
        if (todo == null) {
            throw new TodoNotFoundException(id);
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("todo.message.selectFile"));
            return "redirect:/todo/" + id + "/edit";
        }

        todoAttachmentService.upload(id, file);
        redirectAttributes.addFlashAttribute("successMessage", message("todo.message.attachmentUploaded"));
        return "redirect:/todo/" + id + "/edit";
    }

    /**
     * HTTP GET {@code /todo/attachments/{attachmentId}/download}。
     *
     * <p>添付ファイルをダウンロードします。</p>
     *
     * @param attachmentId 添付ファイルID
     * @param loginUser ログインユーザー
     * @return ファイルレスポンス
     * @throws TodoNotFoundException 添付または紐付くToDoが存在しない場合
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("attachmentId") Long attachmentId,
            @AuthenticationPrincipal LoginUserPrincipal loginUser) {
        TodoAttachmentService.AttachmentDownload attachmentDownload = todoAttachmentService.loadForDownload(attachmentId);
        if (attachmentDownload == null) {
            throw new TodoNotFoundException(attachmentId);
        }

        TodoAttachment attachment = attachmentDownload.attachment();
        Todo todo = todoService.findByIdForAccess(attachment.getTodoId(), loginUser.getId(), loginUser.isAdmin());
        if (todo == null) {
            throw new TodoNotFoundException(attachment.getTodoId());
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getContentType());
            } catch (IllegalArgumentException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(attachmentDownload.resource());
    }

    /**
     * HTTP POST {@code /todo/attachments/{attachmentId}/delete}。
     *
     * <p>添付ファイルを削除します。</p>
     *
     * @param attachmentId 添付ファイルID
     * @param loginUser ログインユーザー
     * @param redirectAttributes リダイレクト属性
     * @return リダイレクト先
     * @throws TodoNotFoundException 添付または紐付くToDoが存在しない場合
     */
    @PostMapping("/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable("attachmentId") Long attachmentId,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        TodoAttachment attachment = todoAttachmentService.findById(attachmentId);
        if (attachment == null) {
            throw new TodoNotFoundException(attachmentId);
        }

        Todo todo = todoService.findByIdForAccess(attachment.getTodoId(), loginUser.getId(), loginUser.isAdmin());
        if (todo == null) {
            throw new TodoNotFoundException(attachment.getTodoId());
        }

        boolean deleted = todoAttachmentService.deleteById(attachmentId);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", message("todo.message.attachmentDeleted"));
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", message("todo.message.attachmentDeleteFailed"));
        }
        return "redirect:/todo/" + attachment.getTodoId() + "/edit";
    }

    /**
     * HTTP POST {@code /todo/{id}/update}。
     *
     * <p>ToDoを更新します。</p>
     *
     * @param id ToDo ID
     * @param title タイトル
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @param loginUser ログインユーザー
     * @param redirectAttributes リダイレクト属性
     * @return リダイレクト先
     * @throws TodoNotFoundException 対象ToDoが存在しない場合
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.isOwner(#id, principal)")
    public String update(@PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @AuthenticationPrincipal LoginUserPrincipal loginUser,
            RedirectAttributes redirectAttributes) {
        boolean updated = todoService.update(id, title, priority, categoryId, deadline, loginUser.getId(),
                loginUser.isAdmin());
        if (!updated) {
            throw new TodoNotFoundException(id);
        }
        redirectAttributes.addFlashAttribute("successMessage", message("todo.message.updated"));
        return "redirect:/todo";
    }

    /**
     * HTTP POST {@code /todo/{id}/toggle}。
     *
     * <p>完了状態を切り替えます。</p>
     *
     * @param id ToDo ID
     * @param loginUser ログインユーザー
     * @return リダイレクト先
     * @throws TodoNotFoundException 対象ToDoが存在しない場合
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN') or @todoSecurityService.isOwner(#id, principal)")
    public String toggle(@PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUserPrincipal loginUser) {
        boolean toggled = todoService.toggleCompleted(id, loginUser.getId(), loginUser.isAdmin());
        if (!toggled) {
            throw new TodoNotFoundException(id);
        }
        return "redirect:/todo";
    }

    /**
     * ToDo一覧をCSV文字列へ変換します。
     *
     * @param todos ToDo一覧
     * @return CSV文字列
     */
    private String buildCsv(List<Todo> todos) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Created By,Status,Created At").append("\r\n");
        for (Todo todo : todos) {
            String status = Boolean.TRUE.equals(todo.getCompleted())
                    ? message("todo.list.status.done")
                    : message("todo.list.status.notDone");
            String createdAt = todo.getCreatedAt() != null ? todo.getCreatedAt().toString() : "";
            sb.append(escapeCsv(String.valueOf(todo.getId()))).append(',')
                    .append(escapeCsv(todo.getTitle())).append(',')
                    .append(escapeCsv(todo.getCreatedBy())).append(',')
                    .append(escapeCsv(status)).append(',')
                    .append(escapeCsv(createdAt)).append("\r\n");
        }
        return sb.toString();
    }

    /**
     * CSV出力向けに値をエスケープします。
     *
     * @param value 変換対象
     * @return CSVエスケープ済み文字列
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * ロケール対応メッセージを取得します。
     *
     * @param code メッセージコード
     * @param args 埋め込み引数
     * @return 解決済みメッセージ
     */
    private String message(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
