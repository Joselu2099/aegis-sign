package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.ContractUseCase;
import com.aegis.sign.domain.exception.ResourceNotFoundException;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.PdfTemplateCompiler;
import com.aegis.sign.domain.service.TemplateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractInteractor implements ContractUseCase {

    private static final String CONTRACT_PATH_FORMAT = "contracts/%s.pdf";

    private final TemplateResolver templateResolver;
    private final PdfTemplateCompiler pdfTemplateCompiler;
    private final StoragePort storagePort;
    private final ContractRepositoryPort contractRepositoryPort;

    @Override
    public Mono<Contract> createContract(String templateId, List<String> signerIds, Map<String, Object> data) {
        if (templateId == null || templateId.isBlank()) {
            return Mono.error(new IllegalArgumentException("templateId is required"));
        }
        if (signerIds == null || signerIds.isEmpty()) {
            return Mono.error(new IllegalArgumentException("signerIds must not be empty"));
        }

        UUID contractId = UUID.randomUUID();
        String storagePath = String.format(CONTRACT_PATH_FORMAT, contractId);

        Map<String, Object> enrichedData = new HashMap<>(data == null ? Map.of() : data);
        enrichedData.put("signers", String.join(", ", signerIds));

        return Mono.fromCallable(() -> templateResolver.resolve(templateId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(template -> Mono.fromCallable(() -> pdfTemplateCompiler.compile(template, enrichedData))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(pdfBytes -> {
                    String hash = pdfTemplateCompiler.calculateHash(pdfBytes);
                    return storagePort.upload(pdfBytes, storagePath)
                            .map(uri -> Contract.builder()
                                    .id(contractId)
                                    .templateId(templateId)
                                    .status(Contract.ContractStatus.PREPARED)
                                    .contentHash(hash)
                                    .uri(uri)
                                    .signerIds(signerIds)
                                    .build());
                })
                .flatMap(contractRepositoryPort::save);
    }

    @Override
    public Mono<Contract> getContract(UUID id) {
        return contractRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Contract not found: " + id)));
    }
}
