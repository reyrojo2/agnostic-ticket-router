package com.agnosticrouter.crm.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_field_mapping")
@Getter @Setter @NoArgsConstructor
public class CrmFieldMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id")
    private CrmOperation operation;
    @Column(name = "dimension_code", nullable = false, length = 80)
    private String dimensionCode;
    @Column(name = "target_field", nullable = false, length = 120)
    private String targetField;
    @Column(name = "value_map_json", nullable = false, columnDefinition = "TEXT")
    private String valueMapJson = "{}";
    @Column(nullable = false)
    private boolean required;
    @Column(nullable = false)
    private boolean active = true;
}
