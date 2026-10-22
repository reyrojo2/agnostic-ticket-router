package com.agnosticrouter.crm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "crm_delivery_audit")
@Getter @Setter @NoArgsConstructor
public class CrmDeliveryAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false, length = 150)
    private String requestId;
    @Column(name = "profile_code", length = 80)
    private String profileCode;
    @Column(name = "integration_code", length = 80)
    private String integrationCode;
    @Column(name = "operation_code", length = 80)
    private String operationCode;
    @Column(nullable = false, length = 32)
    private String outcome;
    @Column(name = "external_id", length = 150)
    private String externalId;
    @Column(name = "http_status")
    private Integer httpStatus;
    private Integer attempts;
    @Column(name = "decision_values_json", columnDefinition = "TEXT")
    private String decisionValuesJson;
    @Column(name = "mapped_fields_json", columnDefinition = "TEXT")
    private String mappedFieldsJson;
    @Column(name = "error_summary", length = 1000)
    private String errorSummary;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
