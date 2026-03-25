package com.example.todo.service;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsyncAlertService {

    private static final Logger log = LoggerFactory.getLogger(AsyncAlertService.class);

    public void notifyAsyncFailure(Throwable ex, Method method, Object[] params) {
        String methodName = method != null ? method.getName() : "unknown";
        int paramCount = params != null ? params.length : 0;
        log.error("ASYNC_ALERT method={} paramCount={} message={}", methodName, paramCount, ex.getMessage(), ex);
    }
}
