package com.aegis.sign.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KycSessionTest {

    @Test
    void approve_ShouldUpdateStatusAndScore() {
        KycSession session = KycSession.builder()
                .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                .build();

        session.approve(0.95);

        assertEquals(KycSession.KycStatus.APPROVED, session.getStatus());
        assertEquals(0.95, session.getFaceMatchScore());
    }

    @Test
    void reject_ShouldUpdateStatus() {
        KycSession session = KycSession.builder()
                .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                .build();

        session.reject();

        assertEquals(KycSession.KycStatus.REJECTED, session.getStatus());
    }
}
