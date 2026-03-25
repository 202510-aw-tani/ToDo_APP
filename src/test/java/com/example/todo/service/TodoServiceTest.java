package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.todo.mapper.TodoHistoryMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoMapper todoMapper;

    @Mock
    private TodoHistoryMapper todoHistoryMapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TodoAttachmentService todoAttachmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("create: ToDoを作成し履歴・監査ログを記録する")
    void create_shouldInsertTodoAndHistory() {
        when(todoHistoryMapper.insert(any())).thenReturn(1);
        doAnswer(invocation -> {
            Todo todo = invocation.getArgument(0, Todo.class);
            todo.setId(100L);
            return 1;
        }).when(todoMapper).insert(any(Todo.class));
        when(notificationService.sendTodoCreatedEmailAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture("mail"));
        when(notificationService.generateTodoReportAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture("report"));

        todoService.create("Write tests", Priority.HIGH, null, LocalDate.of(2026, 3, 31), 1L, "user1");

        verify(todoMapper).insert(any(Todo.class));
        verify(todoHistoryMapper).insert(any());
        verify(auditLogService, times(3)).record(any(), any(), eq("user1"));
    }

    @Test
    @DisplayName("findById: 指定IDのToDoを取得できる")
    void findById_shouldReturnTodo() {
        Todo todo = new Todo();
        todo.setId(1L);
        todo.setTitle("Read a book");
        when(todoMapper.findById(1L)).thenReturn(todo);

        Todo result = todoService.findByIdForApi(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Read a book");
        verify(todoMapper).findById(1L);
    }

    @Test
    @DisplayName("delete: 管理者削除で添付削除・本体削除・履歴記録が実行される")
    void delete_shouldDeleteTodoWithRelatedData() {
        when(todoMapper.deleteById(1L)).thenReturn(1);
        when(todoHistoryMapper.insert(any())).thenReturn(1);

        boolean deleted = todoService.deleteById(1L, 10L, true);

        assertThat(deleted).isTrue();
        verify(todoAttachmentService).deleteAllByTodoId(1L);
        verify(todoMapper).deleteById(1L);
        verify(todoMapper, never()).deleteByIdAndUserId(any(), any());
        verify(todoHistoryMapper).insert(any());
        verify(auditLogService, times(2)).record(any(), any(), any());
    }
}
