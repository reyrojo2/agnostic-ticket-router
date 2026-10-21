package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.domain.model.DecisionProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DecisionProfileRepositoryTest {

    @Autowired
    DecisionProfileRepository repository;

    @Test
    void insertarListarActualizarYEliminar() {
        DecisionProfile profile = new DecisionProfile();
        profile.setCode("TEST_PROFILE");
        profile.setName("Perfil de prueba");
        profile.setVersion(1);
        profile.setActive(true);

        DecisionProfile saved = repository.saveAndFlush(profile);
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findAll()).extracting(DecisionProfile::getCode).contains("TEST_PROFILE");

        saved.setName("Perfil actualizado");
        repository.saveAndFlush(saved);
        assertThat(repository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("Perfil actualizado");

        repository.deleteById(saved.getId());
        repository.flush();
        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
