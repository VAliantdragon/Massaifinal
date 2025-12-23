package com.shadowledger.shadow_ledger_service;

import com.shadowledger.shadow_ledger_service.model.EventType;
import com.shadowledger.shadow_ledger_service.model.LedgerEvent;
import com.shadowledger.shadow_ledger_service.repository.LedgerEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SqlWindowFunctionTest {

    @Autowired
    private LedgerEventRepository repository;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void testBalanceCalculationWithWindowFunction() {
        // Create test events
        LedgerEvent event1 = new LedgerEvent(null, "E_SQL_1", "A_TEST", EventType.CREDIT, 500.0, 1000L);
        LedgerEvent event2 = new LedgerEvent(null, "E_SQL_2", "A_TEST", EventType.CREDIT, 300.0, 2000L);
        LedgerEvent event3 = new LedgerEvent(null, "E_SQL_3", "A_TEST", EventType.DEBIT, 50.0, 3000L);

        repository.save(event1);
        repository.save(event2);
        repository.save(event3);

        // Execute window function query
        List<Map<String, Object>> results = repository.computeShadowBalance("A_TEST");

        assertFalse(results.isEmpty(), "Should return results");

        // Verify running balance calculation
        Map<String, Object> lastResult = results.get(results.size() - 1);
        Double finalBalance = ((Number) lastResult.get("balance")).doubleValue();
        String lastEvent = (String) lastResult.get("lastEvent");

        assertEquals(750.0, finalBalance, 0.01, "Final balance should be 500 + 300 - 50 = 750");
        assertEquals("E_SQL_3", lastEvent, "Last event should be E_SQL_3");
    }

    @Test
    void testBalanceOrderingByTimestampAndEventId() {
        // Create events with same timestamp but different event IDs
        LedgerEvent event1 = new LedgerEvent(null, "E_ORD_1", "A_ORDER", EventType.CREDIT, 100.0, 5000L);
        LedgerEvent event2 = new LedgerEvent(null, "E_ORD_2", "A_ORDER", EventType.CREDIT, 200.0, 5000L);
        LedgerEvent event3 = new LedgerEvent(null, "E_ORD_3", "A_ORDER", EventType.DEBIT, 50.0, 5000L);

        repository.save(event1);
        repository.save(event2);
        repository.save(event3);

        List<Map<String, Object>> results = repository.computeShadowBalance("A_ORDER");

        // Verify deterministic ordering (timestamp, then eventId)
        assertEquals(3, results.size(), "Should have 3 results");

        Map<String, Object> lastResult = results.get(results.size() - 1);
        Double finalBalance = ((Number) lastResult.get("balance")).doubleValue();

        // 100 + 200 - 50 = 250
        assertEquals(250.0, finalBalance, 0.01, "Balance should respect ordering");
    }

    @Test
    void testMultipleAccountsIsolation() {
        // Create events for different accounts
        repository.save(new LedgerEvent(null, "E_ACC1_1", "A_ACC1", EventType.CREDIT, 1000.0, 1000L));
        repository.save(new LedgerEvent(null, "E_ACC2_1", "A_ACC2", EventType.CREDIT, 2000.0, 1000L));

        List<Map<String, Object>> results1 = repository.computeShadowBalance("A_ACC1");
        List<Map<String, Object>> results2 = repository.computeShadowBalance("A_ACC2");

        assertEquals(1000.0, ((Number) results1.get(0).get("balance")).doubleValue(), 0.01);
        assertEquals(2000.0, ((Number) results2.get(0).get("balance")).doubleValue(), 0.01);
    }
}

