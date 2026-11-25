package com.thub.outpost.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for every integration test: the full application, a real Postgres, MockMvc.
 *
 * <pre>
 * class GrantControllerIT extends AbstractIntegrationTest {
 *     &#64;Autowired MockMvc mockMvc;
 *     ...
 * }
 * </pre>
 *
 * The container is a "singleton": it is started once, in the static block, and shared by every
 * subclass for the whole test run. (With the usual {@code @Testcontainers/@Container} setup, each test
 * class would start and stop its own container, which is slow.) Testcontainers' Ryuk helper
 * removes it when the JVM exits.
 *
 * Spring also reuses the same application context for all subclasses that have the same configuration,
 * so keep per-class annotations (like {@code @MockitoBean}) to a minimum to keep tests fast.
 *
 * Pure unit tests (Mockito) must NOT extend this class: they don't need Spring at all.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // @ServiceConnection makes Spring Boot point the datasource at this container, so no URL is configured.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
