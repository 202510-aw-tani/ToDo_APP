package com.example.todo.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.AuditLog;

public interface AuditLogMapper {
    int insert(AuditLog auditLog);

    List<AuditLog> search(@Param("action") String action,
            @Param("entityType") String entityType,
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAtExclusive") LocalDateTime toAtExclusive,
            @Param("limit") int limit);
}
