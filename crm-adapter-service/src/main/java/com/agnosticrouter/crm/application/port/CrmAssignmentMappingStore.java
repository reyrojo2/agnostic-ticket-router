package com.agnosticrouter.crm.application.port;
import com.agnosticrouter.crm.domain.model.CrmAssignmentMapping;
import java.util.Optional;
public interface CrmAssignmentMappingStore {
    java.util.List<CrmAssignmentMapping> findByOperationIdOrderByIdAsc(Long operationId);
    Optional<CrmAssignmentMapping> findById(Long id);
    <S extends CrmAssignmentMapping> S save(S value);
    void deleteById(Long id);
}
