package com.project.auth_service.controller;

import com.project.auth_service.dto.data.AuthenticationData;
import com.project.auth_service.dto.request.LoginRequest;
import com.project.auth_service.dto.request.RegisterRequest;
import com.project.auth_service.dto.response.Base;
import com.project.auth_service.dto.response.LoginResponse;
import com.project.auth_service.dto.response.RegisterResponse;
import com.project.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Base<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return Base.<RegisterResponse>builder()
                .data(userService.register(request))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Base<LoginResponse> login(@RequestBody LoginRequest request) {
        return Base.<LoginResponse>builder()
                .data(userService.login(request))
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping(path = "/validate")
    public Base<AuthenticationData> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authStr) {
        String token = authStr.substring("Bearer ".length());
        return Base.<AuthenticationData>builder()
                .data(userService.validateToken(token))
                .status(HttpStatus.OK.value())
                .build();
    }
}
