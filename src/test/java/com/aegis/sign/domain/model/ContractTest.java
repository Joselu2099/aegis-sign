package com.aegis.sign.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractTest {

    @Test
    void markAsSigned_ShouldUpdateStatus() {
        Contract contract = Contract.builder()
                .status(Contract.ContractStatus.PREPARED)
                .build();

        contract.markAsSigned();

        assertEquals(Contract.ContractStatus.SIGNED, contract.getStatus());
    }

    @Test
    void markAsSigned_ShouldThrowException_WhenAlreadySigned() {
        Contract contract = Contract.builder()
                .status(Contract.ContractStatus.SIGNED)
                .build();

        assertThrows(IllegalStateException.class, contract::markAsSigned);
    }
}
