package com.aegis.sign;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    //@Container
    static PostgreSQLContainer<?> postgres = null;

    //@Container
    static RedisContainer redis = null;

    //@Container
    static GenericContainer<?> minio = null;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
    }

    protected void setupWebTestClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
