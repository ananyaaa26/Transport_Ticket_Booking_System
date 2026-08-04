package com.example.transport.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long ticketId;

    private String username;

    private Long routeId;

    private String source;

    private String destination;

    private String seatNo;

    private String status;

    private LocalDateTime bookingTime;

}