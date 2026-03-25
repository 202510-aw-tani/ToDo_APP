package com.example.todo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.todo.audit.Auditable;
import com.example.todo.exception.BusinessException;
import com.example.todo.mapper.TodoHistoryMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.mapper.UserMapper;
import com.example.todo.model.AppUser;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.model.TodoHistory;

@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);
    private static final long ASYNC_RESULT_TIMEOUT_SECONDS = 3L;

    private final TodoMapper todoMapper;
    private final TodoHistoryMapper todoHistoryMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final TodoAttachmentService todoAttachmentService;
    private final NotificationService notificationService;
    private final MailService mailService;

    public TodoService(TodoMapper todoMapper, TodoHistoryMapper todoHistoryMapper, UserMapper userMapper,
            AuditLogService auditLogService, TodoAttachmentService todoAttachmentService,
            NotificationService notificationService, MailService mailService) {
        this.todoMapper = todoMapper;
        this.todoHistoryMapper = todoHistoryMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
        this.todoAttachmentService = todoAttachmentService;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void create(String title, Priority priority, Long categoryId, LocalDate deadline, Long userId,
            String username) {
        String actor = normalizeActor(username, userId);
        auditLogService.record("TODO_CREATE_START", "title=" + title, actor);

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(userId);
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        todo.setCreatedBy(username);
        todo.setCreatedAt(LocalDate.now());
        todoMapper.insert(todo);

        saveHistory(todo.getId(), "CREATE", "title=" + title, actor);
        queueTodoCreatedMail(userId, title, deadline, actor);
        executeAsyncFollowUp(todo.getId(), title, actor);
        auditLogService.record("TODO_CREATE_SUCCESS", "todoId=" + todo.getId(), actor);
    }

    @Transactional(readOnly = true)
    public Page<Todo> findPage(Pageable pageable, boolean sortByPriority, boolean sortByDeadline, Long categoryId,
            Long userId, boolean admin) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        List<Todo> content;
        long total;

        if (admin) {
            content = todoMapper.findPageForAdmin(limit, offset, sortByPriority, sortByDeadline, categoryId);
            total = todoMapper.countAllForAdmin(categoryId);
        } else {
            content = todoMapper.findPage(limit, offset, sortByPriority, sortByDeadline, categoryId, userId);
            total = todoMapper.countAll(categoryId, userId);
        }
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public List<Todo> findAllForExport(Long userId, boolean admin) {
        return admin ? todoMapper.findAll() : todoMapper.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Todo> findAllForApi() {
        return todoMapper.findAll();
    }

    @Transactional(readOnly = true)
    public List<Todo> findIncompleteByDeadlineRange(LocalDate startDate, LocalDate endDate) {
        return todoMapper.findIncompleteByDeadlineRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Todo findByIdForApi(Long id) {
        return todoMapper.findById(id);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_CREATE_API", entityType = "TODO", useResultAsEntityId = true, afterMethod = "findByIdForApi")
    public Todo createForApi(String title, boolean completed, Priority priority, Long categoryId, LocalDate deadline) {
        String actor = "api";
        auditLogService.record("TODO_CREATE_API_START", "title=" + title, actor);

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(null);
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        todo.setCreatedBy(actor);
        todo.setCreatedAt(LocalDate.now());
        todoMapper.insert(todo);

        saveHistory(todo.getId(), "CREATE", "api create title=" + title, actor);
        executeAsyncFollowUp(todo.getId(), title, actor);
        auditLogService.record("TODO_CREATE_API_SUCCESS", "todoId=" + todo.getId(), actor);
        return todoMapper.findById(todo.getId());
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_UPDATE_API", entityType = "TODO", entityIdArgIndex = 0, beforeMethod = "findByIdForApi", afterMethod = "findByIdForApi")
    public Todo updateForApi(Long id, String title, boolean completed, Priority priority, Long categoryId,
            LocalDate deadline) {
        String actor = "api";
        auditLogService.record("TODO_UPDATE_API_START", "todoId=" + id, actor);

        Todo current = todoMapper.findById(id);
        if (current == null) {
            return null;
        }
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(current.getUserId());
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);
        int updated = todoMapper.updateById(todo);
        if (updated == 0) {
            return null;
        }

        saveHistory(id, "UPDATE", "api update title=" + title, actor);
        auditLogService.record("TODO_UPDATE_API_SUCCESS", "todoId=" + id, actor);
        return todoMapper.findById(id);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_DELETE_API", entityType = "TODO", entityIdArgIndex = 0, beforeMethod = "findByIdForApi")
    public boolean deleteForApi(Long id) {
        String actor = "api";
        auditLogService.record("TODO_DELETE_API_START", "todoId=" + id, actor);

        todoAttachmentService.deleteAllByTodoId(id);
        boolean deleted = todoMapper.deleteById(id) > 0;
        if (deleted) {
            saveHistory(id, "DELETE", "api delete", actor);
            auditLogService.record("TODO_DELETE_API_SUCCESS", "todoId=" + id, actor);
        }
        return deleted;
    }

    @Transactional(readOnly = true)
    public Todo findByIdForAccess(Long id, Long userId, boolean admin) {
        return admin ? todoMapper.findById(id) : todoMapper.findByIdAndUserId(id, userId);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_DELETE", entityType = "TODO", entityIdArgIndex = 0, beforeMethod = "findByIdForApi")
    public boolean deleteById(Long id, Long userId, boolean admin) {
        String actor = normalizeActor(null, userId);
        auditLogService.record("TODO_DELETE_START", "todoId=" + id, actor);

        todoAttachmentService.deleteAllByTodoId(id);
        boolean deleted = admin ? todoMapper.deleteById(id) > 0 : todoMapper.deleteByIdAndUserId(id, userId) > 0;
        if (deleted) {
            saveHistory(id, "DELETE", "deleted by user", actor);
            auditLogService.record("TODO_DELETE_SUCCESS", "todoId=" + id, actor);
        }
        return deleted;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public int deleteByIds(List<Integer> ids, Long userId, boolean admin) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String actor = normalizeActor(null, userId);
        auditLogService.record("TODO_BULK_DELETE_START", "count=" + ids.size(), actor);

        for (Integer id : ids) {
            if (id != null) {
                todoAttachmentService.deleteAllByTodoId(id.longValue());
            }
        }
        int deleted = admin ? todoMapper.deleteByIds(ids) : todoMapper.deleteByIdsAndUserId(ids, userId);
        if (deleted > 0) {
            auditLogService.record("TODO_BULK_DELETE_SUCCESS", "deleted=" + deleted, actor);
        }
        return deleted;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_UPDATE", entityType = "TODO", entityIdArgIndex = 0, beforeMethod = "findByIdForApi", afterMethod = "findByIdForApi")
    public boolean update(Long id, String title, Priority priority, Long categoryId, LocalDate deadline, Long userId,
            boolean admin) {
        String actor = normalizeActor(null, userId);
        auditLogService.record("TODO_UPDATE_START", "todoId=" + id, actor);

        Todo current = findByIdForAccess(id, userId, admin);
        if (current == null) {
            return false;
        }

        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(current.getCompleted());
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setUserId(current.getUserId());
        todo.setCategoryId(categoryId);
        todo.setDeadline(deadline);

        boolean updated = admin ? todoMapper.updateById(todo) > 0 : todoMapper.updateByIdAndUserId(todo) > 0;
        if (updated) {
            saveHistory(id, "UPDATE", "title=" + title, actor);
            auditLogService.record("TODO_UPDATE_SUCCESS", "todoId=" + id, actor);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    @Auditable(action = "TODO_TOGGLE", entityType = "TODO", entityIdArgIndex = 0, beforeMethod = "findByIdForApi", afterMethod = "findByIdForApi")
    public boolean toggleCompleted(Long id, Long userId, boolean admin) {
        Todo todo = findByIdForAccess(id, userId, admin);
        if (todo == null) {
            return false;
        }
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        boolean updated = admin ? todoMapper.updateById(todo) > 0 : todoMapper.updateByIdAndUserId(todo) > 0;
        if (updated) {
            saveHistory(id, "TOGGLE_COMPLETED", "completed=" + todo.getCompleted(), normalizeActor(null, userId));
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return todoMapper.existsById(id) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long id, Long userId) {
        return todoMapper.countByIdAndUserId(id, userId) > 0;
    }

    private void saveHistory(Long todoId, String action, String detail, String actor) {
        TodoHistory history = new TodoHistory();
        history.setTodoId(todoId);
        history.setAction(action);
        history.setDetail(detail);
        history.setActor(actor);
        history.setCreatedAt(LocalDateTime.now());

        if (todoHistoryMapper.insert(history) != 1) {
            throw new IllegalStateException("Failed to insert todo history");
        }
    }

    private void executeAsyncFollowUp(Long todoId, String title, String actor) {
        CompletableFuture<String> emailFuture = notificationService.sendTodoCreatedEmailAsync(actor, title);
        CompletableFuture<String> reportFuture = notificationService.generateTodoReportAsync(todoId, title);
        notificationService.notifyExternalSystemAsync(title);

        try {
            CompletableFuture.allOf(emailFuture, reportFuture).get(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String emailResult = emailFuture.get(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String reportResult = reportFuture.get(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            auditLogService.record("TODO_ASYNC_SUCCESS",
                    "todoId=" + todoId + ", email=" + emailResult + ", report=" + reportResult, actor);
        } catch (TimeoutException ex) {
            log.warn("Async follow-up timeout for todoId={}", todoId, ex);
            auditLogService.record("TODO_ASYNC_TIMEOUT", "todoId=" + todoId + ", timeoutSeconds="
                    + ASYNC_RESULT_TIMEOUT_SECONDS, actor);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Async follow-up interrupted for todoId={}", todoId, ex);
            auditLogService.record("TODO_ASYNC_INTERRUPTED", "todoId=" + todoId, actor);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Async follow-up failed for todoId={} message={}", todoId, cause.getMessage(), cause);
            auditLogService.record("TODO_ASYNC_FAILURE",
                    "todoId=" + todoId + ", reason=" + cause.getClass().getSimpleName(), actor);
        }
    }

    private void queueTodoCreatedMail(Long userId, String title, LocalDate deadline, String actor) {
        if (userId == null) {
            return;
        }

        AppUser user = userMapper.findById(userId);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            auditLogService.record("TODO_MAIL_SKIPPED", "reason=no_email,userId=" + userId, actor);
            return;
        }

        mailService.sendTodoCreatedTextMail(user.getEmail(), user.getUsername(), title, deadline);
        auditLogService.record("TODO_MAIL_QUEUED", "userId=" + userId + ", email=" + user.getEmail(), actor);
    }

    private String normalizeActor(String username, Long userId) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return userId != null ? "user:" + userId : "system";
    }
}
