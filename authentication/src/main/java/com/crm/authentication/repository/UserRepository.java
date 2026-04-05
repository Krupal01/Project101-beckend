package com.crm.authentication.repository;

import com.crm.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    // Social auth lookups — field names match entity: googleUid, appleUid
    Optional<User> findByGoogleUid(String googleUid);
    Optional<User> findByAppleUid(String appleUid);
}