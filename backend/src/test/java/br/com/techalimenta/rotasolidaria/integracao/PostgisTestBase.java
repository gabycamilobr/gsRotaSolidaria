package br.com.techalimenta.rotasolidaria.integracao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base dos testes que rodam contra um PostgreSQL + PostGIS de verdade (com as migrações Flyway e o seed).
 * Só executam quando a variável ROTA_TEST_DB_URL está definida, por exemplo:
 * ROTA_TEST_DB_URL=jdbc:postgresql://localhost:5432/rotasolidaria_test mvn test
 */
@SpringBootTest(properties = "app.agendador.intervalo-ms=3600000")
@EnabledIfEnvironmentVariable(named = "ROTA_TEST_DB_URL", matches = ".+")
abstract class PostgisTestBase {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("ROTA_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> env("ROTA_TEST_DB_USER", "rota"));
        r.add("spring.datasource.password", () -> env("ROTA_TEST_DB_PASSWORD", "rota"));
    }

    private static String env(String nome, String padrao) {
        String v = System.getenv(nome);
        return v == null || v.isBlank() ? padrao : v;
    }
}
