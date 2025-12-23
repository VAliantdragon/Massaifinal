package com.shadowledger.shadow_ledger_service.dto;

import java.math.BigDecimal;

public record EventDto(
        String eventId,
        String accountId,
        String type,
        BigDecimal amount,
        Long timestamp
) {}
