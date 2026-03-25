package com.example.todo.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * ToDo情報を表すエンティティです。
 *
 * <p>一覧表示、CSV出力、期限判定などで利用するドメインモデルです。</p>
 *
 * @author academia
 * @version 1.0
 * @since 1.0
 * @see com.example.todo.service.TodoService
 */
@Data
public class Todo {
    /** ToDo ID。 */
    private Long id;
    /** タイトル。 */
    private String title;
    /** 完了状態。 */
    private Boolean completed;
    /** 優先度。 */
    private Priority priority;
    /** 所有ユーザーID。 */
    private Long userId;
    /** カテゴリID。 */
    private Long categoryId;
    /** カテゴリ情報。 */
    private Category category;
    /** 期限日。 */
    private LocalDate deadline;
    /** 作成者名。 */
    private String createdBy;
    /** 作成日。 */
    private LocalDate createdAt;

    /**
     * 期限超過かどうかを判定します。
     *
     * <p>期限が存在し、かつ現在日付より前なら {@code true} を返します。</p>
     *
     * @return 期限超過の場合は {@code true}
     */
    public boolean isOverdue() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    /**
     * 期限が近いかどうかを判定します。
     *
     * <p>本メソッドでは現在日付から {@code 3} 日以内を「期限間近」とします。</p>
     *
     * @return 期限間近の場合は {@code true}
     */
    public boolean isNearDeadline() {
        if (deadline == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !deadline.isBefore(today) && !deadline.isAfter(today.plusDays(3));
    }

    /**
     * 期限までの残日数を取得します。
     *
     * @return 期限が未設定の場合は {@code null}、設定済みの場合は残日数
     */
    public Long getDaysUntilDeadline() {
        if (deadline == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }
}
