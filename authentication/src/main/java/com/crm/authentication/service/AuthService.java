package com.crm.authentication.service;

import com.crm.authentication.dto.AuthResponse;
import com.crm.authentication.dto.LoginRequest;
import com.crm.authentication.dto.RegisterRequest;
import com.crm.authentication.entity.RefreshToken;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.crm.authentication.repository.UserRepository;
import com.crm.authentication.utils.JwtUtil;
import com.krunish.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BaseException("Email already exists", HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setGlobalStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshTokenValue) {

        RefreshToken refreshToken = tokenService.validateRefreshToken(refreshTokenValue);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user);

        return new AuthResponse(newAccessToken, refreshTokenValue);
    }
}
