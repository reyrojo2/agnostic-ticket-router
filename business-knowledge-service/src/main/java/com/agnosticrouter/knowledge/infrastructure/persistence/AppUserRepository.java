package com.agnosticrouter.knowledge.infrastructure.persistence;
import com.agnosticrouter.knowledge.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppUserRepository extends JpaRepository<AppUser,Long>{ Optional<AppUser> findByUsernameAndActiveTrue(String username); }
