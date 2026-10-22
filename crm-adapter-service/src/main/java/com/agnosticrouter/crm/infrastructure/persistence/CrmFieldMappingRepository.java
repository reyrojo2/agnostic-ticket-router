package com.agnosticrouter.crm.infrastructure.persistence;
import com.agnosticrouter.crm.application.port.CrmFieldMappingStore;
import com.agnosticrouter.crm.domain.model.CrmFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CrmFieldMappingRepository extends JpaRepository<CrmFieldMapping, Long>, CrmFieldMappingStore {
  java.util.List<CrmFieldMapping> findByOperationIdOrderByIdAsc(Long operationId);
}
