package com.example.todo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.todo.mapper.TodoAttachmentMapper;
import com.example.todo.model.TodoAttachment;

@Service
public class TodoAttachmentService {

    private final TodoAttachmentMapper todoAttachmentMapper;
    private final FileStorageService fileStorageService;

    public TodoAttachmentService(TodoAttachmentMapper todoAttachmentMapper, FileStorageService fileStorageService) {
        this.todoAttachmentMapper = todoAttachmentMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TodoAttachment upload(Long todoId, MultipartFile file) {
        FileStorageService.StoredFile storedFile = fileStorageService.store(file);

        TodoAttachment attachment = new TodoAttachment();
        attachment.setTodoId(todoId);
        attachment.setOriginalFilename(storedFile.originalFilename());
        attachment.setStoredFilename(storedFile.storedFilename());
        attachment.setContentType(storedFile.contentType());
        attachment.setFileSize(storedFile.size());
        attachment.setUploadedAt(LocalDateTime.now());

        todoAttachmentMapper.insert(attachment);
        return todoAttachmentMapper.findById(attachment.getId());
    }

    @Transactional(readOnly = true)
    public List<TodoAttachment> findByTodoId(Long todoId) {
        return todoAttachmentMapper.findByTodoId(todoId);
    }

    @Transactional(readOnly = true)
    public TodoAttachment findById(Long id) {
        return todoAttachmentMapper.findById(id);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload loadForDownload(Long id) {
        TodoAttachment attachment = todoAttachmentMapper.findById(id);
        if (attachment == null) {
            return null;
        }
        Resource resource = fileStorageService.loadAsResource(attachment.getStoredFilename());
        return new AttachmentDownload(attachment, resource);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        TodoAttachment attachment = todoAttachmentMapper.findById(id);
        if (attachment == null) {
            return false;
        }

        int deleted = todoAttachmentMapper.deleteById(id);
        if (deleted > 0) {
            fileStorageService.delete(attachment.getStoredFilename());
            return true;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByTodoId(Long todoId) {
        List<TodoAttachment> attachments = todoAttachmentMapper.findByTodoId(todoId);
        for (TodoAttachment attachment : attachments) {
            fileStorageService.delete(attachment.getStoredFilename());
        }
        todoAttachmentMapper.deleteByTodoId(todoId);
    }

    public record AttachmentDownload(TodoAttachment attachment, Resource resource) {
    }
}
