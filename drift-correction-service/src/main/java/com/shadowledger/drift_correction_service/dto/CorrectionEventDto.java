package com.shadowledger.drift_correction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionEventDto {
    private String eventId;
    private String accountId;
    private String type; // credit or debit
    private Double amount;
}