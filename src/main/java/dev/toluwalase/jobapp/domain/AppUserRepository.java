package dev.toluwalase.jobapp.domain;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;



public interface AppUserRepository extends CrudRepository<AppUser, Long> {
    Optional <AppUser> findByUsername(String username);
    Optional <AppUser> findByEmail(String email);
}
