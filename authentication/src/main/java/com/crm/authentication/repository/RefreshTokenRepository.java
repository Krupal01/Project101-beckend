package com.crm.authentication.repository;

import com.crm.authentication.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Entity has @ManyToOne User user — so derived queries use User_Id underscore convention
    List<RefreshToken> findAllByUser_Id(UUID userId);

    /**
     * Bulk-revoke all active tokens for a user.
     * JPQL must navigate the relationship via t.user.id — t.userId does not exist on the entity.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true " +
            "WHERE t.user.id = :userId AND (t.isRevoked = false OR t.isRevoked IS NULL)")
    void revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Hard-delete all tokens for a user (account deletion).
     */
    void deleteAllByUser_Id(UUID userId);
}