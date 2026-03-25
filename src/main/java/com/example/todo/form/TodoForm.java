package com.example.todo.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.todo.model.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ToDo入力画面で使用するフォームクラスです。
 *
 * <p>{@link com.example.todo.controller.TodoController} の入力値を
 * バインドするために使用します。</p>
 *
 * @author academia
 * @version 1.0
 * @since 1.0
 * @see com.example.todo.controller.TodoController
 */
@Data
public class TodoForm {

    /** ToDoタイトル。 */
    @NotBlank(message = "{validation.todo.title.required}")
    @Size(max = 255, message = "{validation.todo.title.max}")
    private String title;

    /** 優先度。 */
    private Priority priority = Priority.MEDIUM;

    /** カテゴリID。 */
    private Long categoryId;

    /** 期限日。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "TodoForm{title='" + title + "', priority=" + priority + ", categoryId=" + categoryId
                + ", deadline=" + deadline + "}";
    }
}
