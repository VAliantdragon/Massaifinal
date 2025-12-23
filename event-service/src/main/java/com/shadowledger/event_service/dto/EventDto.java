package com.shadowledger.event_service.dto;

import java.math.BigDecimal;

public record EventDto(
        String eventId,
        String accountId,
        String type,
        BigDecimal amount,
        long timestamp
) {}