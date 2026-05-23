package com.aegis.sign.infrastructure.adapter.signature;

import com.aegis.sign.domain.port.SignatureServicePort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SignatureServiceAdapter implements SignatureServicePort {

    @Override
    public Mono<String> sign(String contentHash, String certificateThumbprint) {
        // Dummy implementation for now, just return a mocked signature hash
        return Mono.just("signed-" + contentHash + "-" + certificateThumbprint);
    }
}
