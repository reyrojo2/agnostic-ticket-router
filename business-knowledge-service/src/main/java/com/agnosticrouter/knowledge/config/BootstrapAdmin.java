package com.agnosticrouter.knowledge.config;
import com.agnosticrouter.knowledge.domain.model.AppUser;
import com.agnosticrouter.knowledge.infrastructure.persistence.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class BootstrapAdmin implements CommandLineRunner {
 private final AppUserRepository users; private final PasswordEncoder encoder;
 @Value("${app.security.admin-user}") private String username;
 @Value("${app.security.admin-password}") private String password;
 public BootstrapAdmin(AppUserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 @Override public void run(String... args){
   if(users.findByUsernameAndActiveTrue(username).isEmpty()){
     AppUser u=new AppUser();u.setUsername(username);u.setPasswordHash(encoder.encode(password));u.setRole("ADMIN");u.setActive(true);users.save(u);
   }
 }
}
