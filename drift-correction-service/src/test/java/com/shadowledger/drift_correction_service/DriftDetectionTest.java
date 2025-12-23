package com.shadowledger.drift_correction_service;

import com.shadowledger.drift_correction_service.dto.CbsBalanceDto;
import com.shadowledger.drift_correction_service.dto.CorrectionEventDto;
import com.shadowledger.drift_correction_service.service.DriftCorrectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DriftDetectionTest {

    @Autowired
    private DriftCorrectionService driftCorrectionService;

    @Test
    void testDetectNoDrift() {
        CbsBalanceDto cbsBalance = new CbsBalanceDto("A10", 500.0);
        Double shadowBalance = 500.0;

        CorrectionEventDto correction = driftCorrectionService.generateCorrectionEvent(cbsBalance, shadowBalance);

        assertNull(correction, "Should not generate correction when balances match");
    }

    @Test
    void testDetectPositiveDrift() {
        CbsBalanceDto cbsBalance = new CbsBalanceDto("A10", 600.0);
        Double shadowBalance = 500.0;

        CorrectionEventDto correction = driftCorrectionService.generateCorrectionEvent(cbsBalance, shadowBalance);

        assertNotNull(correction, "Should generate correction for positive drift");
        assertEquals("credit", correction.getType(), "Should generate credit correction");
        assertEquals(100.0, correction.getAmount(), 0.01, "Correction amount should be 100");
        assertTrue(correction.getEventId().startsWith("CORR-"), "Event ID should start with CORR-");
    }

    @Test
    void testDetectNegativeDrift() {
        CbsBalanceDto cbsBalance = new CbsBalanceDto("A10", 400.0);
        Double shadowBalance = 500.0;

        CorrectionEventDto correction = driftCorrectionService.generateCorrectionEvent(cbsBalance, shadowBalance);

        assertNotNull(correction, "Should generate correction for negative drift");
        assertEquals("debit", correction.getType(), "Should generate debit correction");
        assertEquals(100.0, correction.getAmount(), 0.01, "Correction amount should be absolute value");
    }

    @Test
    void testManualCorrectionCreation() {
        CorrectionEventDto correction = driftCorrectionService.createManualCorrection("A11", "credit", 50.0);

        assertNotNull(correction);
        assertEquals("A11", correction.getAccountId());
        assertEquals("credit", correction.getType());
        assertEquals(50.0, correction.getAmount());
        assertTrue(correction.getEventId().startsWith("MANUAL-"), "Manual corrections should have MANUAL- prefix");
    }
}

