package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(String email) {
        return taskRepository.findAllByOwnerEmailOrderByCreatedAtDesc(email)
                .stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(String email, TaskRequest request) {
        Task task = new Task();
        task.setOwnerEmail(email);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setCompleted(Boolean.TRUE.equals(request.completed()));
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(String email, Long id, TaskRequest request) {
        Task task = taskRepository.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(String email, Long id) {
        Task task = taskRepository.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        taskRepository.delete(task);
    }
}
