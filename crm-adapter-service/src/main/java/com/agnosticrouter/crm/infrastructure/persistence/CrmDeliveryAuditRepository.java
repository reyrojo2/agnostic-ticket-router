package com.agnosticrouter.crm.infrastructure.persistence;
import com.agnosticrouter.crm.application.port.CrmDeliveryAuditStore;
import com.agnosticrouter.crm.domain.model.CrmDeliveryAudit;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CrmDeliveryAuditRepository extends JpaRepository<CrmDeliveryAudit, Long>, CrmDeliveryAuditStore {
  java.util.List<CrmDeliveryAudit> findTop50ByOrderByCreatedAtDesc();
}
