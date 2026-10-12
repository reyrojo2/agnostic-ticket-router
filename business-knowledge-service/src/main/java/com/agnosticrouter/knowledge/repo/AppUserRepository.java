package com.agnosticrouter.knowledge.repo;
import com.agnosticrouter.knowledge.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppUserRepository extends JpaRepository<AppUser,Long>{ Optional<AppUser> findByUsernameAndActiveTrue(String username); }
