package com.shadowledger.shadow_ledger_service.controller;

import com.shadowledger.shadow_ledger_service.dto.ShadowBalance;
import com.shadowledger.shadow_ledger_service.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/{accountId}/shadow-balance")
    public ResponseEntity<ShadowBalance> getShadowBalance(@PathVariable String accountId) {
        ShadowBalance balance = ledgerService.getShadowBalance(accountId);
        return ResponseEntity.ok(balance);
    }
}

