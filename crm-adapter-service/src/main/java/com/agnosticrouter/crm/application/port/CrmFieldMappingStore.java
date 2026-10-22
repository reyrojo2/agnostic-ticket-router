package com.agnosticrouter.crm.application.port;
import com.agnosticrouter.crm.domain.model.CrmFieldMapping;
import java.util.Optional;
public interface CrmFieldMappingStore {
    java.util.List<CrmFieldMapping> findByOperationIdOrderByIdAsc(Long operationId);
    Optional<CrmFieldMapping> findById(Long id);
    <S extends CrmFieldMapping> S save(S value);
    void deleteById(Long id);
}
