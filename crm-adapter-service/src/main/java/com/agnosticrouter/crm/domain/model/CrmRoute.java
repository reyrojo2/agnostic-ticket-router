package com.agnosticrouter.crm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_route")
@Getter
@Setter
@NoArgsConstructor
public class CrmRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_code", nullable = false, length = 80)
    private String profileCode;

    @Column(name = "dimension_code", length = 80)
    private String dimensionCode;

    @Column(name = "option_code", length = 80)
    private String optionCode;

    @Column(name = "integration_code", nullable = false, length = 80)
    private String integrationCode;

    @Column(name = "operation_code", nullable = false, length = 80)
    private String operationCode = "CREATE_TICKET";

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean active = true;
}
