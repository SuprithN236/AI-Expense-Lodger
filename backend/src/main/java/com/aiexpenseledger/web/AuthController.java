package com.aiexpenseledger.web;

import com.aiexpenseledger.monitoring.LogMonitoringClient;
import com.aiexpenseledger.service.AuthService;
import com.aiexpenseledger.service.AuthService.AuthResult;
import com.aiexpenseledger.web.dto.AuthResponse;
import com.aiexpenseledger.web.dto.LoginRequest;
import com.aiexpenseledger.web.dto.SignupRequest;
import com.aiexpenseledger.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final LogMonitoringClient monitoring;

    public AuthController(AuthService authService, LogMonitoringClient monitoring) {
        this.authService = authService;
        this.monitoring = monitoring;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        AuthResult result = authService.signup(request.email(), request.password());
        monitoring.info(LogMonitoringClient.SERVICE_AUTH, "New account registered: user " + result.user().getId());
        return toResponse(result);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        monitoring.info(LogMonitoringClient.SERVICE_AUTH, "User " + result.user().getId() + " signed in");
        return toResponse(result);
    }

    private static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), "Bearer", result.expiresInSeconds(), UserResponse.from(result.user()));
    }
}
