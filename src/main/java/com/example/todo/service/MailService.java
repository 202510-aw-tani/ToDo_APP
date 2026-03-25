package com.example.todo.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.todo.model.Todo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Async("emailExecutor")
    public void sendTodoCreatedTextMail(String to, String username, String todoTitle, LocalDate deadline) {
        if (!canSend(to)) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("[ToDo] Created: " + todoTitle);
        message.setText(buildTodoCreatedTextBody(username, todoTitle, deadline));
        mailSender.send(message);
        log.info("Todo-created text mail sent to={}", to);
    }

    @Async("emailExecutor")
    public void sendTodoCreatedHtmlMail(String to, String username, String todoTitle, LocalDate deadline) {
        if (!canSend(to)) {
            return;
        }
        Context context = new Context(Locale.JAPAN);
        context.setVariable("username", username);
        context.setVariable("todoTitle", todoTitle);
        context.setVariable("deadline", deadline);

        String htmlBody = templateEngine.process("mail/todo-created", context);
        String textBody = buildTodoCreatedTextBody(username, todoTitle, deadline);
        sendHtmlMultipartMail(to, "[ToDo] Created: " + todoTitle, textBody, htmlBody);
        log.info("Todo-created HTML mail sent to={}", to);
    }

    @Async("emailExecutor")
    public void sendDeadlineReminderHtmlMail(String to, String username, List<Todo> todos, LocalDate today) {
        if (!canSend(to) || todos == null || todos.isEmpty()) {
            return;
        }
        Context context = new Context(Locale.JAPAN);
        context.setVariable("username", username);
        context.setVariable("todos", todos);
        context.setVariable("today", today);

        String htmlBody = templateEngine.process("mail/deadline-reminder", context);
        String textBody = buildReminderTextBody(username, todos, today);
        sendHtmlMultipartMail(to, "[ToDo] Deadline reminder", textBody, htmlBody);
        log.info("Deadline reminder mail sent to={} count={}", to, todos.size());
    }

    private void sendHtmlMultipartMail(String to, String subject, String textBody, String htmlBody) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to compose MIME mail", ex);
        }
    }

    private boolean canSend(String to) {
        if (!StringUtils.hasText(to)) {
            log.warn("Skip mail: recipient is empty");
            return false;
        }
        if (!StringUtils.hasText(fromAddress)) {
            log.warn("Skip mail: app.mail.from is not configured");
            return false;
        }
        return true;
    }

    private String buildTodoCreatedTextBody(String username, String todoTitle, LocalDate deadline) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(username).append(",").append("\n\n");
        body.append("A new ToDo has been created.").append("\n");
        body.append("Title: ").append(todoTitle).append("\n");
        body.append("Deadline: ").append(deadline != null ? deadline : "N/A").append("\n\n");
        body.append("ToDo App");
        return body.toString();
    }

    private String buildReminderTextBody(String username, List<Todo> todos, LocalDate today) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(username).append(",").append("\n\n");
        body.append("This is your deadline reminder for ").append(today).append(".").append("\n");
        for (Todo todo : todos) {
            body.append("- ").append(todo.getTitle())
                    .append(" (deadline: ").append(todo.getDeadline()).append(")")
                    .append("\n");
        }
        body.append("\nToDo App");
        return body.toString();
    }
}
