package com.example.todo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.todo.mapper.AuditLogMapper;
import com.example.todo.model.AuditLog;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordAudit(String action, String entityType, Long entityId, Long userId, String oldValue,
            String newValue, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUserId(userId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String action, String entityType, Long userId, String keyword, LocalDate fromDate,
            LocalDate toDate, int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 500);
        LocalDateTime fromAt = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toAtExclusive = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
        return auditLogMapper.search(trimToNull(action), trimToNull(entityType), userId, trimToNull(keyword), fromAt,
                toAtExclusive, normalizedLimit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String eventType, String detail, String actor) {
        Long userId = parseUserId(actor);
        String payload = "{\"legacyActor\":\"" + safeJson(actor) + "\",\"detail\":\"" + safeJson(detail) + "\"}";
        recordAudit(eventType, "LEGACY_EVENT", null, userId, null, payload, null);
    }

    private Long parseUserId(String actor) {
        if (!StringUtils.hasText(actor)) {
            return null;
        }
        if (!actor.startsWith("user:")) {
            return null;
        }
        try {
            return Long.valueOf(actor.substring("user:".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
