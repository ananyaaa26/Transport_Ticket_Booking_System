package com.example.transport.ticket.controller;

import com.example.transport.ticket.dto.BookingResponse;
import com.example.transport.ticket.feign.RouteDTO;
import com.example.transport.ticket.feign.VehicleServiceClient;
import com.example.transport.ticket.model.Ticket;
import com.example.transport.ticket.model.User;
import com.example.transport.ticket.repository.TicketRepository;
import com.example.transport.ticket.repository.UserRepository;
import com.example.transport.ticket.service.BookingService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@CrossOrigin
public class TicketController {

    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final VehicleServiceClient vehicleServiceClient;

    public TicketController(
            BookingService bookingService,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            VehicleServiceClient vehicleServiceClient
    ) {
        this.bookingService = bookingService;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.vehicleServiceClient = vehicleServiceClient;
    }

    @PostMapping
    public ResponseEntity<?> bookTicket(
            @RequestBody BookRequest request,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        Optional<Ticket> newTicket = bookingService.bookTicket(
                user.getId(),
                request.getRouteId(),
                request.getSeatNo()
        );

        return newTicket
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.badRequest().body(
                                Map.of("error", "Seat not available or route is full.")
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicketById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();

        if (!ticket.getUserId().equals(user.getId())
                && !user.getRole().equals("ROLE_ADMIN")) {

            return ResponseEntity.status(403)
                    .body(Map.of("error", "Forbidden"));
        }

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/booked-seats/{routeId}")
    public ResponseEntity<List<String>> getBookedSeats(
            @PathVariable Long routeId
    ) {

        List<String> bookedSeatNumbers =
                bookingService.getBookedSeatsForRoute(routeId)
                        .stream()
                        .map(Ticket::getSeatNo)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(bookedSeatNumbers);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BookingResponse>> getAllBookings(
            Authentication authentication
    ) {

        User currentUser = (User) authentication.getPrincipal();

        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }

        List<BookingResponse> bookings = ticketRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(Ticket::getBookingTime)
                                .reversed()
                )
                .map(ticket -> {

                    RouteDTO route = vehicleServiceClient
                            .getRouteById(ticket.getRouteId())
                            .orElse(null);

                    return BookingResponse.builder()
                            .ticketId(ticket.getId())

                            .username(
                                    userRepository.findById(ticket.getUserId())
                                            .map(User::getUsername)
                                            .orElse("Unknown User")
                            )

                            .routeId(ticket.getRouteId())

                            .source(
                                    route != null
                                            ? route.getSource()
                                            : "-"
                            )

                            .destination(
                                    route != null
                                            ? route.getDestination()
                                            : "-"
                            )

                            .seatNo(ticket.getSeatNo())

                            .status(ticket.getStatus())

                            .bookingTime(ticket.getBookingTime())

                            .build();

                })
                .toList();

        return ResponseEntity.ok(bookings);
    }
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            Authentication authentication
    ) {

        User currentUser = (User) authentication.getPrincipal();

        List<BookingResponse> bookings = ticketRepository
                .findByUserId(currentUser.getId())
                .stream()
                .sorted(
                        Comparator.comparing(Ticket::getBookingTime)
                                .reversed()
                )
                .map(ticket -> {

                    RouteDTO route = vehicleServiceClient
                            .getRouteById(ticket.getRouteId())
                            .orElse(null);

                    return BookingResponse.builder()

                            .ticketId(ticket.getId())

                            .routeId(ticket.getRouteId())

                            .source(
                                    route != null
                                            ? route.getSource()
                                            : "-"
                            )

                            .destination(
                                    route != null
                                            ? route.getDestination()
                                            : "-"
                            )

                            .seatNo(ticket.getSeatNo())

                            .status(ticket.getStatus())

                            .bookingTime(ticket.getBookingTime())

                            .build();

                })
                .toList();

        return ResponseEntity.ok(bookings);

    }
    @Data
    static class BookRequest {

        private Long routeId;

        private String seatNo;

    }
}