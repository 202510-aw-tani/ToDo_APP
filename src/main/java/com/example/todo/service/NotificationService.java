package com.example.todo.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async("emailExecutor")
    public CompletableFuture<String> sendTodoCreatedEmailAsync(String recipient, String todoTitle) {
        simulateWork(400L);
        String message = "Mail sent to " + recipient + " for todo=" + todoTitle;
        log.info(message);
        return CompletableFuture.completedFuture(message);
    }

    @Async("taskExecutor")
    public CompletableFuture<String> generateTodoReportAsync(Long todoId, String todoTitle) {
        simulateWork(700L);
        String report = "Report generated for todoId=" + todoId + ", title=" + todoTitle;
        log.info(report);
        return CompletableFuture.completedFuture(report);
    }

    @Async("taskExecutor")
    public void notifyExternalSystemAsync(String todoTitle) {
        if (todoTitle != null && todoTitle.toLowerCase().contains("force-async-error")) {
            throw new IllegalStateException("External notification failed: " + todoTitle);
        }
        log.info("External notification queued for todo={}", todoTitle);
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Async work interrupted", ex);
        }
    }
}
