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

/**
 * ToDoの業務ロジックを提供するサービスです。
 *
 * <p>本クラスは {@link TodoMapper} を使ったCRUD、監査ログ、履歴記録、
 * 非同期後処理（メール・レポート）を統合します。</p>
 *
 * @author academia
 * @version 1.0
 * @since 1.0
 * @see com.example.todo.controller.TodoController
 */
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

    /**
     * コンストラクタです。
     *
     * @param todoMapper ToDo Mapper
     * @param todoHistoryMapper 履歴Mapper
     * @param userMapper ユーザーMapper
     * @param auditLogService 監査ログサービス
     * @param todoAttachmentService 添付ファイルサービス
     * @param notificationService 非同期通知サービス
     * @param mailService メール送信サービス
     */
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

    /**
     * ToDoを新規作成します。
     *
     * @param title タイトル
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @param userId 作成ユーザーID
     * @param username 作成ユーザー名
     * @throws IllegalStateException 履歴保存に失敗した場合
     */
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

    /**
     * ページング付きToDo一覧を取得します。
     *
     * @param pageable ページング情報
     * @param sortByPriority 優先度ソートを有効にする場合は {@code true}
     * @param sortByDeadline 期限ソートを有効にする場合は {@code true}
     * @param categoryId カテゴリID
     * @param userId ユーザーID
     * @param admin 管理者フラグ
     * @return ToDoページ
     */
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

    /**
     * CSVエクスポート用のToDo一覧を取得します。
     *
     * @param userId ユーザーID
     * @param admin 管理者フラグ
     * @return エクスポート対象ToDo一覧
     */
    @Transactional(readOnly = true)
    public List<Todo> findAllForExport(Long userId, boolean admin) {
        return admin ? todoMapper.findAll() : todoMapper.findAllByUserId(userId);
    }

    /**
     * API向けに全ToDoを取得します。
     *
     * @return ToDo一覧
     */
    @Transactional(readOnly = true)
    public List<Todo> findAllForApi() {
        return todoMapper.findAll();
    }

    /**
     * 指定期間内で未完了かつ期限付きのToDoを取得します。
     *
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 条件に一致するToDo一覧
     */
    @Transactional(readOnly = true)
    public List<Todo> findIncompleteByDeadlineRange(LocalDate startDate, LocalDate endDate) {
        return todoMapper.findIncompleteByDeadlineRange(startDate, endDate);
    }

    /**
     * API向けにID指定でToDoを取得します。
     *
     * @param id ToDo ID
     * @return ToDo。存在しない場合は {@code null}
     */
    @Transactional(readOnly = true)
    public Todo findByIdForApi(Long id) {
        return todoMapper.findById(id);
    }

    /**
     * API向けにToDoを作成します。
     *
     * @param title タイトル
     * @param completed 完了フラグ
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @return 作成後のToDo
     * @throws IllegalStateException 履歴保存に失敗した場合
     */
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

    /**
     * API向けにToDoを更新します。
     *
     * @param id ToDo ID
     * @param title タイトル
     * @param completed 完了フラグ
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @return 更新後のToDo。対象が存在しない場合は {@code null}
     */
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

    /**
     * API向けにToDoを削除します。
     *
     * @param id ToDo ID
     * @return 削除成功時は {@code true}
     */
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

    /**
     * アクセス権を考慮してToDoを取得します。
     *
     * @param id ToDo ID
     * @param userId ユーザーID
     * @param admin 管理者フラグ
     * @return 参照可能なToDo。存在しない、または権限がない場合は {@code null}
     */
    @Transactional(readOnly = true)
    public Todo findByIdForAccess(Long id, Long userId, boolean admin) {
        return admin ? todoMapper.findById(id) : todoMapper.findByIdAndUserId(id, userId);
    }

    /**
     * ToDoを1件削除します。
     *
     * @param id ToDo ID
     * @param userId 操作ユーザーID
     * @param admin 管理者フラグ
     * @return 削除成功時は {@code true}
     */
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

    /**
     * ToDoを一括削除します。
     *
     * @param ids 削除対象ID一覧
     * @param userId 操作ユーザーID
     * @param admin 管理者フラグ
     * @return 削除件数
     */
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

    /**
     * ToDoを更新します。
     *
     * @param id ToDo ID
     * @param title タイトル
     * @param priority 優先度
     * @param categoryId カテゴリID
     * @param deadline 期限日
     * @param userId 操作ユーザーID
     * @param admin 管理者フラグ
     * @return 更新成功時は {@code true}
     */
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

    /**
     * 完了状態をトグルします。
     *
     * @param id ToDo ID
     * @param userId 操作ユーザーID
     * @param admin 管理者フラグ
     * @return 更新成功時は {@code true}
     */
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

    /**
     * ToDoの存在有無を確認します。
     *
     * @param id ToDo ID
     * @return 存在する場合は {@code true}
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return todoMapper.existsById(id) > 0;
    }

    /**
     * 指定ユーザーがToDoの所有者かどうかを判定します。
     *
     * @param id ToDo ID
     * @param userId ユーザーID
     * @return 所有者である場合は {@code true}
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long id, Long userId) {
        return todoMapper.countByIdAndUserId(id, userId) > 0;
    }

    /**
     * 操作履歴を保存します。
     *
     * @param todoId ToDo ID
     * @param action 操作種別
     * @param detail 詳細
     * @param actor 操作者
     * @throws IllegalStateException 履歴登録に失敗した場合
     */
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

    /**
     * 非同期後処理（通知メール・レポート生成）を実行します。
     *
     * @param todoId ToDo ID
     * @param title タイトル
     * @param actor 操作者
     */
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

    /**
     * ToDo作成通知メールを非同期キューへ投入します。
     *
     * @param userId ユーザーID
     * @param title タイトル
     * @param deadline 期限日
     * @param actor 操作者
     */
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

    /**
     * 監査ログ用の操作者名を正規化します。
     *
     * @param username ユーザー名
     * @param userId ユーザーID
     * @return ユーザー名があればその値、なければ {@code user:<ID>} か {@code system}
     */
    private String normalizeActor(String username, Long userId) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return userId != null ? "user:" + userId : "system";
    }
}
