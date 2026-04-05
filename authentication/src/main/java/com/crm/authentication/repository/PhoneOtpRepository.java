package com.crm.authentication.repository;

import com.crm.authentication.entity.PhoneOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PhoneOtpRepository extends JpaRepository<PhoneOtp, UUID> {

    /**
     * Finds the latest OTP row for a phone + purpose combination.
     * usedAt IS NULL means it hasn't been consumed yet.
     */
    Optional<PhoneOtp> findTopByPhoneAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            String phone, String purpose);

    /**
     * Deletes all OTP rows for a phone+purpose after successful verification
     * so stale codes can't be replayed.
     */
    @Modifying
    @Query("DELETE FROM PhoneOtp o WHERE o.phone = :phone AND o.purpose = :purpose")
    void deleteAllByPhoneAndPurpose(@Param("phone") String phone,
                                    @Param("purpose") String purpose);
}