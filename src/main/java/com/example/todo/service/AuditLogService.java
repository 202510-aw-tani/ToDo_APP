package com.example.todo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo.mapper.AuditLogMapper;
import com.example.todo.model.AuditLog;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String eventType, String detail, String actor) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setDetail(detail);
        log.setActor(actor);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}
