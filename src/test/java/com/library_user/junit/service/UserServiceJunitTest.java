package com.library_user.junit.service;

import com.library_user.exceptions.CustomException;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import com.library_user.repository.UserRepository;
import com.library_user.security.JwtUtil;
import com.library_user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceJunitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UUID userId;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testEmail = "john.doe@example.com";
        testPassword = "encodedPassword";

        testUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email(testEmail)
                .password(testPassword)
                .contact("1234567890")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();
    }

    // Should authenticate a valid PATRON and return AuthResponse
    @Test
    void whenAuthenticateWithValidCredentials_thenReturnAuthResponse() {
        AuthRequest request = new AuthRequest(testEmail, "password");
        String token = "jwt.token.here";
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn(token);
        AuthResponse response = userService.authenticate(request);
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // Should throw exception for invalid credentials
    @Test
    void whenAuthenticateWithInvalidCredentials_thenThrowException() {
        AuthRequest request = new AuthRequest(testEmail, "wrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> userService.authenticate(request))
                .isInstanceOf(CustomException.class);
    }

    // Should register a new PATRON successfully
    @Test
    void whenRegisterWithNewEmail_thenReturnUserResponse() {
        RegisterRequest request = new RegisterRequest(
                "John Doe",
                testEmail,
                "password",
                "1234567890",
                Role.PATRON
        );
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn(testPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        UserResponse response = userService.register(request);
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(testEmail);
        assertThat(response.name()).isEqualTo("John Doe");
        verify(userRepository).save(any(User.class));
    }

    // Should throw exception when registering with existing email
    @Test
    void whenRegisterWithExistingEmail_thenThrowException() {
        RegisterRequest request = new RegisterRequest(
                "John Doe",
                testEmail,
                "password",
                "1234567890",
                Role.PATRON
        );
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class);
    }

    // Should return PATRON by ID
    @Test
    void whenGetUserById_thenReturnUserResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        UserResponse response = userService.getUserById(userId);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(testEmail);
    }

    // Should throw exception when user is not found by ID
    @Test
    void whenGetUserByIdWithInvalidId_thenThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(CustomException.class);
    }

    // Should return all users (PATRONS and LIBRARIANS)
    @Test
    void whenGetAllUsers_thenReturnUserResponseList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        List<UserResponse> response = userService.getAllUsers(pageable);
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(userId);
    }

    // Should update PATRON user info successfully
    @Test
    void whenUpdateUser_thenReturnUpdatedUserResponse() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                "updated@example.com",
                "newPassword",
                "9876543210",
                Role.PATRON,
                1
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("updated@example.com", userId)).thenReturn(false);
        when(userRepository.existsByIdAndRole(userId, Role.LIBRARIAN)).thenReturn(false);
        User updatedUser = User.builder()
                .id(userId)
                .name(request.name())
                .email(request.email())
                .password("encodedNewPassword")
                .contact(request.contact())
                .role(request.role())
                .borrowedBookCount(request.borrowedBookCount())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        UserResponse response = userService.updateUser(userId, request);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.email()).isEqualTo("updated@example.com");
        verify(userRepository).save(any(User.class));
    }

    // Should throw exception when updated email already exists
    @Test
    void whenUpdateUserWithExistingEmail_thenThrowException() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                "existing@example.com",
                "newPassword",
                "9876543210",
                Role.PATRON,
                1
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("existing@example.com", userId)).thenReturn(true);
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(CustomException.class);
    }

    // Should throw exception when trying to update a LIBRARIAN
    @Test
    void whenUpdateLibrarian_thenThrowException() {
        testUser.setRole(Role.LIBRARIAN);
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name for Librarian",
                testEmail,
                "newPassword",
                "9876543210",
                Role.LIBRARIAN,
                1
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByIdAndRole(userId, Role.LIBRARIAN)).thenReturn(true);
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(CustomException.class);
    }

    // Should delete existing PATRON user
    @Test
    void whenDeleteUser_thenDeleteSuccessfully() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById(userId);
        userService.deleteUser(userId);
        verify(userRepository).deleteById(userId);
    }

    // Should throw exception when deleting non-existent user
    @Test
    void whenDeleteNonExistentUser_thenThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(CustomException.class);
    }
}
