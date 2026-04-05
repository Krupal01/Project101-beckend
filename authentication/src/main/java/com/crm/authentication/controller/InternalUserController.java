package com.crm.authentication.controller;

import com.crm.authentication.dto.InternalUserResponse;
import com.crm.authentication.dto.UpdateKycStatusRequest;
import com.crm.authentication.dto.UpdateUserStatusRequest;
import com.crm.authentication.service.UserService;
import com.krunish.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal controller — NOT exposed to end users.
 * Protected by X-SERVICE-TOKEN header validation (via security filter or interceptor).
 * Used by other microservices (admin panel, KYC service, moderator tools, etc.)
 */
@RestController
@RequestMapping("/internal/auth/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * Fetches core user identity data for inter-service communication.
     * e.g. order-service verifying user role and status before placing an order.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InternalUserResponse>> getUserById(
            @PathVariable UUID id) {

        InternalUserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates a user's global status (ACTIVE / SUSPENDED / BANNED / DEACTIVATED).
     * Called by the admin panel.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        userService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Updates a user's KYC status (NONE / PENDING / APPROVED / REJECTED).
     * Called by the KYC / moderator service.
     */
    @PutMapping("/{id}/kyc")
    public ResponseEntity<ApiResponse<Void>> updateKycStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKycStatusRequest request) {

        userService.updateKycStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}