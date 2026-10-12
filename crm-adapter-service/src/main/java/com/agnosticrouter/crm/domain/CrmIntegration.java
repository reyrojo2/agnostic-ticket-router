package com.agnosticrouter.crm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_integration")
@Getter
@Setter
@NoArgsConstructor
public class CrmIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    @Column(name = "auth_type", nullable = false, length = 30)
    private String authType = "NONE";

    @Column(name = "username_ref", length = 500)
    private String usernameRef;

    @Column(name = "secret_ref", length = 500)
    private String secretRef;

    @Column(name = "api_key_name", length = 160)
    private String apiKeyName;

    @Column(name = "token_url", length = 1000)
    private String tokenUrl;

    @Column(length = 500)
    private String scope;

    @Column(name = "default_headers_json", columnDefinition = "TEXT")
    private String defaultHeadersJson;

    @Column(nullable = false)
    private Boolean active = true;
}
