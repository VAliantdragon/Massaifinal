package com.shadowledger.drift_correction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CbsBalanceDto {
    private String accountId;
    private Double reportedBalance;
}
