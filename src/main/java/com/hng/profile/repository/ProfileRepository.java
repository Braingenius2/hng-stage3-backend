package com.hng.profile.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hng.profile.model.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {

  Optional<Profile> findByNameIgnoreCase(String name);

  @Query("select lower(p.name) from Profile p where lower(p.name) in :names")
  Set<String> findExistingLowerCaseNames(@Param("names") Set<String> names);
}
