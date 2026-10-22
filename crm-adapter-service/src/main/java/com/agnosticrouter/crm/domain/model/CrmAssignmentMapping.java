package com.agnosticrouter.crm.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_assignment_mapping")
@Getter @Setter @NoArgsConstructor
public class CrmAssignmentMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id")
    private CrmOperation operation;
    @Column(name = "dimension_code", nullable = false, length = 80)
    private String dimensionCode;
    @Column(name = "option_code", nullable = false, length = 120)
    private String optionCode;
    @Column(name = "assignment_kind", nullable = false, length = 12)
    private String assignmentKind;
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;
    @Column(name = "external_name", length = 160)
    private String externalName;
    @Column(nullable = false)
    private boolean active = true;
}
