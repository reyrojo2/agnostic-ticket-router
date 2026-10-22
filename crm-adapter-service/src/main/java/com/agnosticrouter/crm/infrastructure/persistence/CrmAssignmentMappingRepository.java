package com.agnosticrouter.crm.infrastructure.persistence;
import com.agnosticrouter.crm.application.port.CrmAssignmentMappingStore;
import com.agnosticrouter.crm.domain.model.CrmAssignmentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CrmAssignmentMappingRepository extends JpaRepository<CrmAssignmentMapping, Long>, CrmAssignmentMappingStore {
  java.util.List<CrmAssignmentMapping> findByOperationIdOrderByIdAsc(Long operationId);
}
