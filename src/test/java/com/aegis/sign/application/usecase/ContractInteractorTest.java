package com.aegis.sign.application.usecase;

import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.PdfTemplateCompiler;
import com.aegis.sign.domain.service.TemplateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractInteractorTest {

    @Mock
    private TemplateResolver templateResolver;
    @Mock
    private PdfTemplateCompiler pdfTemplateCompiler;
    @Mock
    private StoragePort storagePort;
    @Mock
    private ContractRepositoryPort contractRepositoryPort;

    private ContractInteractor contractInteractor;

    @BeforeEach
    void setUp() {
        contractInteractor = new ContractInteractor(
                templateResolver,
                pdfTemplateCompiler,
                storagePort,
                contractRepositoryPort
        );
    }

    @Test
    void createContract_ShouldReturnPreparedContract() {
        // Arrange
        String templateId = "template-123";
        List<String> signerIds = List.of("signer-1", "signer-2");
        Map<String, Object> data = Map.of("key", "value");

        String templateString = "templateContent";
        byte[] pdfBytes = "pdfContent".getBytes();
        String pdfHash = "hash123";
        String uri = "s3://contracts/12345.pdf";

        when(templateResolver.resolve(templateId)).thenReturn(templateString);
        when(pdfTemplateCompiler.compile(eq(templateString), any(Map.class))).thenReturn(pdfBytes);
        when(pdfTemplateCompiler.calculateHash(pdfBytes)).thenReturn(pdfHash);
        when(storagePort.upload(eq(pdfBytes), any(String.class))).thenReturn(Mono.just(uri));
        when(contractRepositoryPort.save(any(Contract.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<Contract> result = contractInteractor.createContract(templateId, signerIds, data);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(contract -> {
                    return contract.getTemplateId().equals(templateId) &&
                            contract.getStatus() == Contract.ContractStatus.PREPARED &&
                            contract.getContentHash().equals(pdfHash) &&
                            contract.getUri().equals(uri) &&
                            contract.getSignerIds().equals(signerIds) &&
                            contract.getId() != null;
                })
                .verifyComplete();

        verify(templateResolver).resolve(templateId);
        verify(pdfTemplateCompiler).compile(eq(templateString), any(Map.class));
        verify(pdfTemplateCompiler).calculateHash(pdfBytes);
        verify(storagePort).upload(eq(pdfBytes), any(String.class));
        verify(contractRepositoryPort).save(any(Contract.class));
    }

    @Test
    void createContract_WhenTemplateIdIsBlank_ShouldReturnError() {
        // Act
        Mono<Contract> result = contractInteractor.createContract("  ", List.of("signer-1"), Map.of());

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("templateId is required")
                .verify();
    }

    @Test
    void createContract_WhenTemplateIdIsNull_ShouldReturnError() {
        // Act
        Mono<Contract> result = contractInteractor.createContract(null, List.of("signer-1"), Map.of());

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("templateId is required")
                .verify();
    }

    @Test
    void createContract_WhenSignerIdsIsEmpty_ShouldReturnError() {
        // Act
        Mono<Contract> result = contractInteractor.createContract("template-123", List.of(), Map.of());

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("signerIds must not be empty")
                .verify();
    }

    @Test
    void createContract_WhenSignerIdsIsNull_ShouldReturnError() {
        // Act
        Mono<Contract> result = contractInteractor.createContract("template-123", null, Map.of());

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("signerIds must not be empty")
                .verify();
    }

    @Test
    void getContract_ShouldReturnContractFromRepository() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        Contract contract = Contract.builder()
                .id(contractId)
                .templateId("template-123")
                .status(Contract.ContractStatus.PREPARED)
                .build();

        when(contractRepositoryPort.findById(contractId)).thenReturn(Mono.just(contract));

        // Act
        Mono<Contract> result = contractInteractor.getContract(contractId);

        // Assert
        StepVerifier.create(result)
                .expectNext(contract)
                .verifyComplete();

        verify(contractRepositoryPort).findById(contractId);
    }
}
