package com.example.transport.ticket.service;

import com.example.transport.ticket.feign.RouteDTO;
import com.example.transport.ticket.feign.VehicleServiceClient;
import com.example.transport.ticket.model.Ticket;
import com.example.transport.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- Required Import
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final TicketRepository ticketRepository;
    private final VehicleServiceClient vehicleServiceClient;

    public BookingService(TicketRepository ticketRepository, VehicleServiceClient vehicleServiceClient) {
        this.ticketRepository = ticketRepository;
        this.vehicleServiceClient = vehicleServiceClient;
    }

    /**
     * Checking Availability -> Checking Capacity -> Seat Booking
     * wrapped securely inside a single transactional rollback boundary.
     */
    @Transactional // <-- Bundles these database reads and writes as a single atomic operation
    public Optional<Ticket> bookTicket(Long userId, Long routeId, String seatNo) {
        // 1. Check if the seat is already booked for the given route
        if (ticketRepository.existsByRouteIdAndSeatNo(routeId, seatNo)) {
            return Optional.empty(); // Seat is already taken
        }

        // 2. Call vehicle-service via Feign Client to get route details and check capacity.
        Optional<RouteDTO> routeOpt = vehicleServiceClient.getRouteById(routeId);

        if (routeOpt.isEmpty() || routeOpt.get().getVehicle() == null) {
            return Optional.empty(); // Route not found or has no vehicle attached
        }

        RouteDTO route = routeOpt.get();
        long currentBookings = ticketRepository.findByRouteId(routeId).size();

        // 3. Check if the vehicle has reached its maximum passenger capacity
        if (currentBookings >= route.getVehicle().getCapacity()) {
            return Optional.empty(); // Vehicle capacity limit reached
        }

        // 4. If all validations pass, create, persist, and confirm the new ticket
        Ticket newTicket = Ticket.builder()
                .userId(userId)
                .routeId(routeId)
                .seatNo(seatNo)
                .status("BOOKED")
                .bookingTime(LocalDateTime.now())
                .build();

        return Optional.of(ticketRepository.save(newTicket));
    }

    public List<Ticket> getBookedSeatsForRoute(Long routeId) {
        return ticketRepository.findByRouteId(routeId);
    }
}