package com.example.todo.aspect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.todo.audit.Auditable;
import com.example.todo.security.LoginUserPrincipal;
import com.example.todo.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object aroundAuditableMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long entityId = resolveEntityIdFromArgs(args, auditable.entityIdArgIndex());
        Object beforeSnapshot = resolveSnapshot(joinPoint.getTarget(), auditable.beforeMethod(), entityId, args, null);

        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            Long finalEntityId = entityId;
            if (finalEntityId == null && auditable.useResultAsEntityId()) {
                finalEntityId = resolveEntityIdFromResult(result);
            }

            Object afterSnapshot;
            if (throwable != null) {
                Map<String, Object> errorMap = new LinkedHashMap<>();
                errorMap.put("exception", throwable.getClass().getSimpleName());
                errorMap.put("message", throwable.getMessage());
                afterSnapshot = errorMap;
            } else {
                afterSnapshot = resolveSnapshot(joinPoint.getTarget(), auditable.afterMethod(), finalEntityId, args,
                        result);
            }

            try {
                auditLogService.recordAudit(
                        auditable.action(),
                        auditable.entityType(),
                        finalEntityId,
                        resolveCurrentUserId(),
                        toJson(beforeSnapshot),
                        toJson(afterSnapshot),
                        resolveClientIp());
            } catch (Exception ex) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                log.error("Failed to write audit log for method={}", signature.toShortString(), ex);
            }
        }
    }

    private Object resolveSnapshot(Object target, String snapshotMethodName, Long entityId, Object[] args, Object result) {
        if (StringUtils.hasText(snapshotMethodName)) {
            return invokeSnapshotMethod(target, snapshotMethodName, entityId);
        }
        if (result != null) {
            return result;
        }
        return args;
    }

    private Object invokeSnapshotMethod(Object target, String methodName, Long entityId) {
        Method method = findMethod(target.getClass(), methodName, entityId != null);
        if (method == null) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("snapshotError", "method_not_found");
            map.put("method", methodName);
            return map;
        }

        try {
            if (method.getParameterCount() == 0) {
                return method.invoke(target);
            }
            return method.invoke(target, entityId);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("snapshotError", ex.getClass().getSimpleName());
            map.put("method", methodName);
            map.put("message", ex.getMessage());
            return map;
        }
    }

    private Method findMethod(Class<?> targetClass, String methodName, boolean withEntityId) {
        Method fallback = null;
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (withEntityId && method.getParameterCount() == 1) {
                Class<?> type = method.getParameterTypes()[0];
                if (type.equals(Long.class) || type.equals(long.class) || Number.class.isAssignableFrom(type)) {
                    return method;
                }
                fallback = method;
            } else if (!withEntityId && method.getParameterCount() == 0) {
                return method;
            } else if (method.getParameterCount() == 0) {
                fallback = method;
            }
        }
        return fallback;
    }

    private Long resolveEntityIdFromArgs(Object[] args, int index) {
        if (index < 0 || args == null || index >= args.length) {
            return null;
        }
        return toLong(args[index]);
    }

    private Long resolveEntityIdFromResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object idValue = getId.invoke(result);
            return toLong(idValue);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserPrincipal loginUserPrincipal) {
            return loginUserPrincipal.getId();
        }
        return null;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"jsonError\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
