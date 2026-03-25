package com.example.todo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.todo.mapper.UserMapper;
import com.example.todo.model.AppUser;
import com.example.todo.model.Todo;

@Component
public class TodoReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TodoReminderScheduler.class);

    private final TodoService todoService;
    private final UserMapper userMapper;
    private final MailService mailService;
    private final int daysAhead;

    public TodoReminderScheduler(TodoService todoService,
            UserMapper userMapper,
            MailService mailService,
            @Value("${app.reminder.days-ahead:3}") int daysAhead) {
        this.todoService = todoService;
        this.userMapper = userMapper;
        this.mailService = mailService;
        this.daysAhead = daysAhead;
    }

    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}", zone = "${app.reminder.zone:Asia/Tokyo}")
    public void sendDailyDeadlineReminders() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        List<Todo> nearDeadlineTodos = todoService.findIncompleteByDeadlineRange(today, endDate);

        Map<Long, List<Todo>> groupedByUser = nearDeadlineTodos.stream()
                .filter(todo -> todo.getUserId() != null)
                .collect(Collectors.groupingBy(Todo::getUserId));

        groupedByUser.forEach((userId, todos) -> {
            AppUser user = userMapper.findById(userId);
            if (user == null || !StringUtils.hasText(user.getEmail())) {
                log.info("Skip reminder mail: userId={} has no valid email", userId);
                return;
            }
            mailService.sendDeadlineReminderHtmlMail(user.getEmail(), user.getUsername(), todos, today);
            log.info("Reminder mail queued: userId={} count={}", userId, todos.size());
        });
    }
}
