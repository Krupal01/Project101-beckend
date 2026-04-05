package com.crm.authentication.service;

import com.crm.authentication.dto.InternalUserResponse;
import com.crm.authentication.dto.UpdateKycStatusRequest;
import com.crm.authentication.dto.UpdateUserStatusRequest;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.crm.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns core identity data for inter-service calls.
     * Used by InternalUserController — not exposed to end users.
     */
    public InternalUserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found"));

        return new InternalUserResponse(
                user.getId(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getGlobalStatus(),
                user.getKycStatus()
        );
    }

    /**
     * Updates a user's global status (ACTIVE / SUSPENDED / BANNED / DEACTIVATED).
     * Called by the admin panel via InternalUserController.
     */
    public void updateStatus(UUID id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found"));

        user.setGlobalStatus(request.status());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Updates a user's KYC status (NONE / PENDING / APPROVED / REJECTED).
     * Called by the KYC / moderator service via InternalUserController.
     */
    public void updateKycStatus(UUID id, UpdateKycStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found"));

        user.setKycStatus(request.kycStatus());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}