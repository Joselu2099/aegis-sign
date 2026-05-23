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
@Testcontainers
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("aegis_db")
            .withUsername("aegis_user")
            .withPassword("aegis_password");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio"))
            .withEnv("MINIO_ACCESS_KEY", "aegis_admin")
            .withEnv("MINIO_SECRET_KEY", "aegis_admin_password")
            .withCommand("server /data")
            .withExposedPorts(9000);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Postgres R2DBC
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s", 
                postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // Postgres Flyway (JDBC)
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // MinIO
        registry.add("minio.endpoint", () -> String.format("http://%s:%d", minio.getHost(), minio.getMappedPort(9000)));
        registry.add("minio.access-key", () -> "aegis_admin");
        registry.add("minio.secret-key", () -> "aegis_admin_password");
        registry.add("minio.bucket", () -> "aegis-sign");
    }

    protected void setupWebTestClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
