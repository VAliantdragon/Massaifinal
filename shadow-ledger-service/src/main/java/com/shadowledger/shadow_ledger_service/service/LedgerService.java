package com.shadowledger.shadow_ledger_service.service;


import com.shadowledger.shadow_ledger_service.dto.ShadowBalance;
import com.shadowledger.shadow_ledger_service.repository.LedgerEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LedgerService {

    private final LedgerEventRepository repository;

    public LedgerService(LedgerEventRepository repository) {
        this.repository = repository;
    }

    public ShadowBalance getShadowBalance(String accountId) {
        List<Map<String, Object>> results = repository.computeShadowBalance(accountId);
        if (results.isEmpty()) return new ShadowBalance(accountId, 0.0, null);

        Map<String, Object> last = results.get(results.size() - 1);
        return new ShadowBalance(
                accountId,
                ((Number) last.get("balance")).doubleValue(),
                (String) last.get("lastEvent")
        );
    }
}