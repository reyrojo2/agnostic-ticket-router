package com.agnosticrouter.knowledge.domain.model;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
@Entity @Table(name="decision_dimension") @Getter @Setter @NoArgsConstructor
public class DecisionDimension {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="profile_id") private DecisionProfile profile;
 @Column(nullable=false,length=80) private String code;
 @Column(nullable=false,length=160) private String name;
 @Column(name="primitive_type",nullable=false,length=20) private String primitiveType="CHOICE";
 @Column(nullable=false,columnDefinition="TEXT") private String instructions;
 @Column(name="execution_order",nullable=false) private Integer executionOrder=100;
 @Column(nullable=false) private Boolean required=true;
 @Column(name="auto_min_probability",nullable=false) private Double autoMinProbability=.70;
 @Column(name="min_margin",nullable=false) private Double minMargin=.20;
 @Column(nullable=false) private Boolean active=true;
 @Column(name="depends_on_dimension_code",length=80) private String dependsOnDimensionCode;
 @Column(name="depends_on_option_code",length=80) private String dependsOnOptionCode;
}
