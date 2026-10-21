package com.agnosticrouter.knowledge.domain.model;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
@Entity @Table(name="decision_rule") @Getter @Setter @NoArgsConstructor
public class DecisionRule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="dimension_id") private DecisionDimension dimension;
 @Column(nullable=false,length=120) private String code;
 @Column(name="target_option_code",nullable=false,length=80) private String targetOptionCode;
 @Column(name="rule_type",nullable=false,length=30) private String ruleType;
 @Column(nullable=false,columnDefinition="TEXT") private String pattern;
 @Column(nullable=false,length=20) private String strength="SOFT";
 @Column(nullable=false) private Integer priority=100;
 @Column(nullable=false) private Boolean active=true;
}
