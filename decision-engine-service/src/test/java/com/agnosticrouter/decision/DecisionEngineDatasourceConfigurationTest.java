package com.agnosticrouter.decision;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineDatasourceConfigurationTest {
    @Test
    void datasourceAndSchemaInitializationAreConfiguredForExternalPostgres() throws Exception {
        PropertySource<?> config = new YamlPropertySourceLoader()
                .load("decision-engine", new ClassPathResource("application.yml")).get(0);

        assertThat(config.getProperty("spring.datasource.url")).isEqualTo("${DB_URL:jdbc:postgresql://localhost:5432/agnostic_router}");
        assertThat(config.getProperty("spring.datasource.username")).isEqualTo("${DB_USER}");
        assertThat(config.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(config.getProperty("spring.sql.init.mode")).isEqualTo("always");
        assertThat(config.getProperty("spring.sql.init.schema-locations")).isEqualTo("classpath:schema.sql");
        assertThat(new ClassPathResource("schema.sql").exists()).isTrue();
    }
}
