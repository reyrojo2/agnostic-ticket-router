package com.agnosticrouter.knowledge.domain;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="app_user") @Getter @Setter @NoArgsConstructor
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String username;
 @Column(name="password_hash",nullable=false,length=120) private String passwordHash;
 @Column(nullable=false,length=40) private String role="ADMIN";
 @Column(nullable=false) private Boolean active=true;
}
