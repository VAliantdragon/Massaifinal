package com.shadowledger.drift_correction_service.controller;



import com.shadowledger.drift_correction_service.dto.CbsBalanceDto;
import com.shadowledger.drift_correction_service.dto.CorrectionEventDto;
import com.shadowledger.drift_correction_service.service.DriftCorrectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
public class DriftCorrectionController {

    private final DriftCorrectionService service;

    public DriftCorrectionController(DriftCorrectionService service) {
        this.service = service;
    }

    @PostMapping("/drift-check")
    public ResponseEntity<List<CorrectionEventDto>> checkDrift(@RequestBody List<CbsBalanceDto> balances) {
        List<CorrectionEventDto> corrections = balances.stream()
                .map(b -> service.generateCorrectionEvent(b, service.getShadowBalance(b.getAccountId())))
                .filter(c -> c != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(corrections);
    }

    @PostMapping("/correct/{accountId}")
    public ResponseEntity<CorrectionEventDto> manualCorrection(
            @PathVariable String accountId,
            @RequestParam String type,
            @RequestParam Double amount) {
        CorrectionEventDto correction = service.createManualCorrection(accountId, type, amount);
        return ResponseEntity.ok(correction);
    }
}