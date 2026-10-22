package com.agnosticrouter.crm.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_operation")
@Getter
@Setter
@NoArgsConstructor
public class CrmOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_id")
    private CrmIntegration integration;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "http_method", nullable = false, length = 12)
    private String httpMethod = "POST";

    @Column(name = "path_template", nullable = false, length = 1000)
    private String pathTemplate;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType = "application/json";

    @Column(name = "headers_template_json", columnDefinition = "TEXT")
    private String headersTemplateJson;

    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "success_status_codes", nullable = false, length = 120)
    private String successStatusCodes = "200,201,202";

    @Column(name = "external_id_path", length = 500)
    private String externalIdPath;

    @Column(name = "external_number_path", length = 500)
    private String externalNumberPath;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 15000;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "retry_delay_ms", nullable = false)
    private Long retryDelayMs = 1000L;

    @Column(nullable = false)
    private Boolean active = true;
}
