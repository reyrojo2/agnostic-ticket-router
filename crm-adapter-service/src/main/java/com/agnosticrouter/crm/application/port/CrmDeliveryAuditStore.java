package com.agnosticrouter.crm.application.port;
import com.agnosticrouter.crm.domain.model.CrmDeliveryAudit;
import java.util.Optional;
public interface CrmDeliveryAuditStore {
    java.util.List<CrmDeliveryAudit> findTop50ByOrderByCreatedAtDesc();
    Optional<CrmDeliveryAudit> findById(Long id);
    <S extends CrmDeliveryAudit> S save(S value);
    void deleteById(Long id);
}
