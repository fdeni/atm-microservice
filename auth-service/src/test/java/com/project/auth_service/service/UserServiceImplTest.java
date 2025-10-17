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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Validator validator;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Password123!");
        registerRequest.setName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123!");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("$2a$10$hashedpassword"); // BCrypt hashed password
        user.setName("Test User");
    }


    @Test
    void register_Success() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        try (MockedStatic<BCrypt> bcryptMock = mockStatic(BCrypt.class)) {
            bcryptMock.when(() -> BCrypt.hashpw(anyString(), anyString()))
                    .thenReturn("$2a$10$hashedpassword");
            bcryptMock.when(() -> BCrypt.gensalt()).thenReturn("$2a$10$salt");

            // When
            RegisterResponse response = userService.register(registerRequest);

            // Then
            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
            assertEquals("Test User", response.getName());

            verify(validator, times(1)).validate(registerRequest);
            verify(userRepository, times(1)).existsByUsername(registerRequest.getUsername());
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Test
    void register_UsernameAlreadyExists() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(registerRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());

        verify(validator, times(1)).validate(registerRequest);
        verify(userRepository, times(1)).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ValidationFails() {
        // Given
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        when(validator.validate(any())).thenReturn(violations);

        // When & Then
        assertThrows(
                ConstraintViolationException.class,
                () -> userService.register(registerRequest)
        );

        verify(validator, times(1)).validate(registerRequest);
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordIsHashed() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);

        try (MockedStatic<BCrypt> bcryptMock = mockStatic(BCrypt.class)) {
            String hashedPassword = "$2a$10$hashedpassword";
            bcryptMock.when(() -> BCrypt.gensalt()).thenReturn("$2a$10$salt");
            bcryptMock.when(() -> BCrypt.hashpw(registerRequest.getPassword(), "$2a$10$salt"))
                    .thenReturn(hashedPassword);

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                assertEquals(hashedPassword, savedUser.getPassword());
                return savedUser;
            });

            // When
            userService.register(registerRequest);

            // Then
            bcryptMock.verify(() -> BCrypt.gensalt(), times(1));
            bcryptMock.verify(() -> BCrypt.hashpw(eq(registerRequest.getPassword()), anyString()), times(1));
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Test
    void register_EmptyUsername() {
        // Given
        registerRequest.setUsername("");

        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        when(validator.validate(any())).thenReturn(violations);

        // When & Then
        assertThrows(
                ConstraintViolationException.class,
                () -> userService.register(registerRequest)
        );

        verify(userRepository, never()).existsByUsername(anyString());
    }


    @Test
    void login_Success() {
        // Given
        String token = "jwt.token.here";
        String hashedPassword = BCrypt.hashpw(loginRequest.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn(token);

        // When
        LoginResponse response = userService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getName());
        assertEquals(token, response.getToken());

        verify(validator, times(1)).validate(loginRequest);
        verify(userRepository, times(1)).findByUsername(loginRequest.getUsername());
        verify(jwt, times(1)).generateToken(user);
    }

    @Test
    void login_UserNotFound() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(loginRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid username/password", exception.getReason());

        verify(validator, times(1)).validate(loginRequest);
        verify(userRepository, times(1)).findByUsername(loginRequest.getUsername());
        verify(jwt, never()).generateToken(any(User.class));
    }

    @Test
    void login_InvalidPassword() {
        // Given
        String hashedPassword = BCrypt.hashpw("DifferentPassword123!", BCrypt.gensalt());
        user.setPassword(hashedPassword);

        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(loginRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid username/password", exception.getReason());

        verify(validator, times(1)).validate(loginRequest);
        verify(userRepository, times(1)).findByUsername(loginRequest.getUsername());
        verify(jwt, never()).generateToken(any(User.class));
    }

    @Test
    void login_ValidationFails() {
        // Given
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        when(validator.validate(any())).thenReturn(violations);

        // When & Then
        assertThrows(
                ConstraintViolationException.class,
                () -> userService.login(loginRequest)
        );

        verify(validator, times(1)).validate(loginRequest);
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwt, never()).generateToken(any(User.class));
    }

    @Test
    void login_EmptyPassword() {
        // Given
        loginRequest.setPassword("");

        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        when(validator.validate(any())).thenReturn(violations);

        // When & Then
        assertThrows(
                ConstraintViolationException.class,
                () -> userService.login(loginRequest)
        );

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void login_BCryptCheckPasswordCorrectly() {
        // Given
        String rawPassword = "Password123!";
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        loginRequest.setPassword(rawPassword);

        String token = "jwt.token.here";

        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn(token);

        // When
        LoginResponse response = userService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertTrue(BCrypt.checkpw(rawPassword, hashedPassword)); // Verify BCrypt works correctly
    }


    @Test
    void validateToken_Success() {
        // Given
        String token = "valid.jwt.token";
        AuthenticationData authData = AuthenticationData.builder()
                .id(1L)
                .username("testuser")
                .name("Test User")
                .build();

        when(jwt.validateToken(token)).thenReturn(authData);

        // When
        AuthenticationData result = userService.validateToken(token);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getName());

        verify(jwt, times(1)).validateToken(token);
    }

    @Test
    void validateToken_InvalidToken() {
        // Given
        String token = "invalid.jwt.token";

        when(jwt.validateToken(token)).thenReturn(null);

        // When
        AuthenticationData result = userService.validateToken(token);

        // Then
        assertNull(result);
        verify(jwt, times(1)).validateToken(token);
    }

    @Test
    void validateToken_ExpiredToken() {
        // Given
        String token = "expired.jwt.token";

        when(jwt.validateToken(token)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.validateToken(token)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwt, times(1)).validateToken(token);
    }

    @Test
    void validateToken_EmptyToken() {
        // Given
        String token = "";

        when(jwt.validateToken(token)).thenReturn(null);

        // When
        AuthenticationData result = userService.validateToken(token);

        // Then
        assertNull(result);
        verify(jwt, times(1)).validateToken(token);
    }

    @Test
    void validateToken_NullToken() {
        // Given
        String token = null;

        when(jwt.validateToken(token)).thenReturn(null);

        // When
        AuthenticationData result = userService.validateToken(token);

        // Then
        assertNull(result);
        verify(jwt, times(1)).validateToken(token);
    }


    @Test
    void register_UsernameCaseSensitive() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.existsByUsername("TestUser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        registerRequest.setUsername("TestUser");

        try (MockedStatic<BCrypt> bcryptMock = mockStatic(BCrypt.class)) {
            bcryptMock.when(() -> BCrypt.hashpw(anyString(), anyString()))
                    .thenReturn("$2a$10$hashedpassword");
            bcryptMock.when(() -> BCrypt.gensalt()).thenReturn("$2a$10$salt");

            // When
            userService.register(registerRequest);

            // Then
            verify(userRepository, times(1)).existsByUsername("TestUser");
        }
    }

    @Test
    void login_UsernameCaseSensitive() {
        // Given
        String hashedPassword = BCrypt.hashpw(loginRequest.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setUsername("TestUser");

        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn("token");

        loginRequest.setUsername("TestUser");

        // When
        LoginResponse response = userService.login(loginRequest);

        // Then
        assertEquals("TestUser", response.getUsername());
        verify(userRepository, times(1)).findByUsername("TestUser");
    }

    @Test
    void register_AllFieldsAreMapped() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);

        try (MockedStatic<BCrypt> bcryptMock = mockStatic(BCrypt.class)) {
            bcryptMock.when(() -> BCrypt.hashpw(anyString(), anyString()))
                    .thenReturn("$2a$10$hashedpassword");
            bcryptMock.when(() -> BCrypt.gensalt()).thenReturn("$2a$10$salt");

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                assertEquals(registerRequest.getUsername(), savedUser.getUsername());
                assertEquals(registerRequest.getName(), savedUser.getName());
                assertNotNull(savedUser.getPassword());
                return savedUser;
            });

            // When
            userService.register(registerRequest);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Test
    void login_TokenIsGeneratedCorrectly() {
        // Given
        String hashedPassword = BCrypt.hashpw(loginRequest.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        String expectedToken = "generated.jwt.token.12345";

        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn(expectedToken);

        // When
        LoginResponse response = userService.login(loginRequest);

        // Then
        assertEquals(expectedToken, response.getToken());
        verify(jwt, times(1)).generateToken(user);
    }
}
