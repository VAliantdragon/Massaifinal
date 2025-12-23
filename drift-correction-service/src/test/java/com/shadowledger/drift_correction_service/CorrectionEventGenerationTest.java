package com.shadowledger.drift_correction_service;

import com.shadowledger.drift_correction_service.dto.CorrectionEventDto;
import com.shadowledger.drift_correction_service.service.DriftCorrectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(topics = {"transactions.corrections"}, partitions = 1)
class CorrectionEventGenerationTest {

    @Autowired
    private DriftCorrectionService driftCorrectionService;

    @Test
    void testCorrectionEventGeneration() {
        CorrectionEventDto correction = driftCorrectionService.createManualCorrection(
                "A_TEST", "credit", 100.0
        );

        assertNotNull(correction);
        assertEquals("A_TEST", correction.getAccountId());
        assertEquals("credit", correction.getType());
        assertEquals(100.0, correction.getAmount());
        assertNotNull(correction.getEventId());
        assertTrue(correction.getEventId().contains("A_TEST"), "Event ID should contain account ID");
    }

    @Test
    void testCorrectionEventIdUniqueness() {
        CorrectionEventDto correction1 = driftCorrectionService.createManualCorrection(
                "A_UNIQUE", "credit", 50.0
        );

        // Sleep briefly to ensure different timestamps in event ID
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        CorrectionEventDto correction2 = driftCorrectionService.createManualCorrection(
                "A_UNIQUE", "credit", 50.0
        );

        assertNotEquals(correction1.getEventId(), correction2.getEventId(),
                "Each correction should have unique event ID");
    }

    @Test
    void testCorrectionEventTypesValid() {
        CorrectionEventDto creditCorrection = driftCorrectionService.createManualCorrection(
                "A_TYPE", "credit", 100.0
        );
        CorrectionEventDto debitCorrection = driftCorrectionService.createManualCorrection(
                "A_TYPE", "debit", 50.0
        );

        assertEquals("credit", creditCorrection.getType());
        assertEquals("debit", debitCorrection.getType());
    }
}


