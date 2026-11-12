package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);
    Optional<Task> findByIdAndOwner_Id(Long id, Long ownerId);
}
