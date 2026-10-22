package com.agnosticrouter.decision.infrastructure.persistence;

import com.agnosticrouter.decision.application.port.AiProviderStore;
import com.agnosticrouter.decision.domain.model.AiProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class JdbcAiProviderStore implements AiProviderStore {
    private static final String TABLE = "decision_engine.ai_provider";
    private final JdbcTemplate jdbc;
    private final RowMapper<AiProvider> row = (r, n) -> new AiProvider(r.getLong("id"), r.getString("name"), r.getString("base_url"),
            r.getString("endpoint_path"), r.getString("model"), r.getString("language"), r.getString("auth_type"),
            r.getString("api_key_header"), r.getString("encrypted_api_key"), r.getBoolean("enabled"), r.getObject("default_slot") != null);
    public JdbcAiProviderStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<AiProvider> findAll() { return jdbc.query("SELECT * FROM " + TABLE + " ORDER BY id", row); }
    public Optional<AiProvider> findById(Long id) { return jdbc.query("SELECT * FROM " + TABLE + " WHERE id=?", row, id).stream().findFirst(); }
    public Optional<AiProvider> findDefault() { return jdbc.query("SELECT * FROM " + TABLE + " WHERE default_slot=1 AND enabled=true", row).stream().findFirst(); }
    public AiProvider save(AiProvider p) {
        if (p.id() != null) {
            int updated = jdbc.update("UPDATE " + TABLE + " SET name=?,base_url=?,endpoint_path=?,model=?,language=?,auth_type=?,api_key_header=?,encrypted_api_key=?,enabled=? WHERE id=?",
                    p.name(), p.baseUrl(), p.endpointPath(), p.model(), p.language(), p.authType(), p.apiKeyHeader(), p.encryptedApiKey(), p.enabled(), p.id());
            if (updated == 0) throw new NoSuchElementException("Proveedor no encontrado.");
            return findById(p.id()).orElseThrow();
        }
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO " + TABLE + " (name,base_url,endpoint_path,model,language,auth_type,api_key_header,encrypted_api_key,enabled) VALUES (?,?,?,?,?,?,?,?,?)", new String[]{"id"});
            statement.setString(1, p.name()); statement.setString(2, p.baseUrl()); statement.setString(3, p.endpointPath());
            statement.setString(4, p.model()); statement.setString(5, p.language()); statement.setString(6, p.authType());
            statement.setString(7, p.apiKeyHeader()); statement.setString(8, p.encryptedApiKey()); statement.setBoolean(9, p.enabled());
            return statement;
        }, key);
        return findById(java.util.Objects.requireNonNull(key.getKey()).longValue()).orElseThrow();
    }
    @Transactional
    public void selectDefault(Long id) {
        jdbc.update("UPDATE " + TABLE + " SET default_slot=NULL WHERE default_slot=1");
        if (jdbc.update("UPDATE " + TABLE + " SET default_slot=1 WHERE id=? AND enabled=true", id) == 0) throw new IllegalArgumentException("El proveedor no existe o está deshabilitado.");
    }
    public void clearDefault() { jdbc.update("UPDATE " + TABLE + " SET default_slot=NULL WHERE default_slot=1"); }
    public void delete(Long id) {
        if (jdbc.update("DELETE FROM " + TABLE + " WHERE id=? AND default_slot IS NULL", id) == 0) throw new IllegalArgumentException("El proveedor no existe o está en uso.");
    }
}
