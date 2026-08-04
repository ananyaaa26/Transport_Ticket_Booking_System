package com.example.transport.ticket.repository;

import com.example.transport.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByRouteId(Long routeId);

    List<Ticket> findByUserId(Long userId);

    boolean existsByRouteIdAndSeatNo(Long routeId,String seatNo);

}