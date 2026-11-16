package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findByAppSourceOrderByCreatedAtDesc(String appSource);
    List<Ticket> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    Optional<Ticket> findByTicketCode(String ticketCode);
}
