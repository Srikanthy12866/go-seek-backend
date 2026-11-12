package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.AppUser;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(String email) {
        AppUser owner = findUser(email);
        return taskRepository.findAllByOwner_IdOrderByCreatedAtDesc(owner.getId())
                .stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(String email, TaskRequest request) {
        AppUser owner = findUser(email);
        Task task = new Task();
        task.setOwner(owner);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setCompleted(Boolean.TRUE.equals(request.completed()));
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(String email, Long id, TaskRequest request) {
        AppUser owner = findUser(email);
        Task task = taskRepository.findByIdAndOwner_Id(id, owner.getId())
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
        AppUser owner = findUser(email);
        Task task = taskRepository.findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        taskRepository.delete(task);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists."));
    }
}
