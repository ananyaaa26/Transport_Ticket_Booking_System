package com.example.transport.ticket.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingConfirmationResponse {

    private List<Long> ticketIds;

    private Long routeId;

    private List<String> seats;

    private LocalDateTime bookingTime;

    private String source;

    private String destination;

}