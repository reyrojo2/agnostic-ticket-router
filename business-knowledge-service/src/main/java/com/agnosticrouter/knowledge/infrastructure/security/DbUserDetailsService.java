package com.agnosticrouter.knowledge.infrastructure.security;
import com.agnosticrouter.knowledge.infrastructure.persistence.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class DbUserDetailsService implements UserDetailsService {
 private final AppUserRepository users; public DbUserDetailsService(AppUserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
   var u=users.findByUsernameAndActiveTrue(username).orElseThrow(() -> new UsernameNotFoundException(username));
   return User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles(u.getRole()).build();
 }
}
