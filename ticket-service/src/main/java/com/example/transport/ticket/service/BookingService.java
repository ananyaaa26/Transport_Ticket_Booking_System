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
import java.util.ArrayList;

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
    @Transactional
    public List<Ticket> bookTickets(
            Long userId,
            Long routeId,
            List<String> seatNos
    ) {

        Optional<RouteDTO> routeOpt =
                vehicleServiceClient.getRouteById(routeId);

        if (routeOpt.isEmpty()
                || routeOpt.get().getVehicle() == null) {

            throw new RuntimeException("Route not found.");

        }

        RouteDTO route = routeOpt.get();

        long currentBookings =
                ticketRepository.findByRouteId(routeId).size();

        if (currentBookings + seatNos.size()
                > route.getVehicle().getCapacity()) {

            throw new RuntimeException(
                    "Not enough seats available."
            );

        }

        for (String seat : seatNos) {

            if (ticketRepository.existsByRouteIdAndSeatNo(routeId, seat)) {

                throw new RuntimeException(
                        "Seat " + seat + " is already booked."
                );

            }

        }

        LocalDateTime bookingTime = LocalDateTime.now();

        List<Ticket> bookedTickets = new ArrayList<>();

        for (String seat : seatNos) {

            Ticket ticket = Ticket.builder()
                    .userId(userId)
                    .routeId(routeId)
                    .seatNo(seat)
                    .status("BOOKED")
                    .bookingTime(bookingTime)
                    .build();

            bookedTickets.add(
                    ticketRepository.save(ticket)
            );

        }

        return bookedTickets;

    }

    public List<Ticket> getBookedSeatsForRoute(Long routeId) {
        return ticketRepository.findByRouteId(routeId);
    }
}