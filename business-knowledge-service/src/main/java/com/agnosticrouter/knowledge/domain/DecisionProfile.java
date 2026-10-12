package com.agnosticrouter.knowledge.domain;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="decision_profile") @Getter @Setter @NoArgsConstructor
public class DecisionProfile {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String code;
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false) private Integer version=1;
 @Column(nullable=false) private Boolean active=true;
}
