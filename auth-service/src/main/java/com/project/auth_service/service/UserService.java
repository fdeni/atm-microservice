package com.project.auth_service.service;

import com.project.auth_service.dto.data.AuthenticationData;
import com.project.auth_service.dto.request.LoginRequest;
import com.project.auth_service.dto.request.RegisterRequest;
import com.project.auth_service.dto.response.LoginResponse;
import com.project.auth_service.dto.response.RegisterResponse;

public interface UserService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    AuthenticationData validateToken(String token);
}
