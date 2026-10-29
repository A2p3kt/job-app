package dev.toluwalase.jobapp.domain;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface MasterProfileRepository extends CrudRepository<MasterProfile, Long> {

    Optional<MasterProfile> findByUserId(Long userId);
    Optional<MasterProfile> findByUserUsername(String username);

}
