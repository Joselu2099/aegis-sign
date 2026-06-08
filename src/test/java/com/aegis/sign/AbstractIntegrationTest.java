package com.aegis.sign;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.vault.enabled=false",
        "spring.config.import=",
        "db.username=test",
        "db.password=test",
        "keystore.password=changeit",
        "keystore.key-password=changeit",
        "minio.access-key=minioadmin",
        "minio.secret-key=minioadmin",
        "minio.bucket=aegis-sign",
        "minio.temp-bucket=aegis-sign-temp"
    }
)
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    static boolean dockerAvailable = false;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-03-30T09-41-56Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data")
            .withExposedPorts(9000);

    static {
        if (!Boolean.parseBoolean(System.getProperty("testcontainers.disabled"))) {
            try {
                postgres.start();
                redis.start();
                minio.start();
                dockerAvailable = true;
            } catch (Exception e) {
                // Docker not available — integration tests will be skipped
            }
        }
    }

    @BeforeAll
    static void requireDocker() {
        if (!Boolean.parseBoolean(System.getProperty("testcontainers.disabled"))) {
            assumeTrue(dockerAvailable, "Skipping integration test: Docker not available");
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!Boolean.parseBoolean(System.getProperty("testcontainers.disabled"))) {
            registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
            registry.add("spring.r2dbc.username", postgres::getUsername);
            registry.add("spring.r2dbc.password", postgres::getPassword);
            registry.add("spring.flyway.url", postgres::getJdbcUrl);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
            
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

            registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
            registry.add("minio.access-key", () -> "minioadmin");
            registry.add("minio.secret-key", () -> "minioadmin");
        }
    }

    protected void setupWebTestClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
