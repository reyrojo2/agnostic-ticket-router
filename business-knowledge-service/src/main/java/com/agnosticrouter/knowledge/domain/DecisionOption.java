package com.agnosticrouter.knowledge.domain;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
@Entity @Table(name="decision_option") @Getter @Setter @NoArgsConstructor
public class DecisionOption {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="dimension_id") private DecisionDimension dimension;
 @Column(nullable=false,length=80) private String code;
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false,columnDefinition="TEXT") private String description;
 @Column(name="include_context",columnDefinition="TEXT") private String includeContext;
 @Column(name="exclude_context",columnDefinition="TEXT") private String excludeContext;
 @Column(nullable=false) private Integer priority=100;
 @Column(nullable=false) private Boolean active=true;
 @Column(name="depends_on_dimension_code",length=80) private String dependsOnDimensionCode;
 @Column(name="depends_on_option_code",length=80) private String dependsOnOptionCode;
}
