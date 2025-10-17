package com.project.auth_service.service;

import com.project.auth_service.dto.data.AuthenticationData;
import com.project.auth_service.dto.request.LoginRequest;
import com.project.auth_service.dto.request.RegisterRequest;
import com.project.auth_service.dto.response.LoginResponse;
import com.project.auth_service.dto.response.RegisterResponse;
import com.project.auth_service.entity.User;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.utils.Jwt;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final Validator validator;
    private final Jwt jwt;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        validateRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setName(request.getName());

        User savedUserData = userRepository.save(user);

        return RegisterResponse.builder()
                .username(savedUserData.getUsername())
                .name(savedUserData.getName())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        validateRequest(request);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username/password")
                );

        if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            String token = jwt.generateToken(user);

            return LoginResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .name(user.getName())
                    .token(token)
                    .build();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username/password");
        }
    }

    @Override
    public AuthenticationData validateToken(String token) {
        return jwt.validateToken(token);
    }

    private void validateRequest(Object request) {
        Set<ConstraintViolation<Object>> validation = validator.validate(request);
        if (!validation.isEmpty()) {
            throw new ConstraintViolationException(validation);
        }
    }
}
