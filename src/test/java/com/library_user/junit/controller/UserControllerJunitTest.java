package com.library_user.junit.controller;

import com.library_user.controller.UserController;
import com.library_user.exceptions.CustomException;
import com.library_user.model.entity.Role;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import com.library_user.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerJunitTest {

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private UpdateUserRequest updateRequest;
    private UserResponse userResponse;
    private AuthResponse authResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        registerRequest = new RegisterRequest(
                "John Doe",
                "john.doe@example.com",
                "password123",
                "1234567890",
                Role.PATRON
        );

        authRequest = new AuthRequest(
                "john.doe@example.com",
                "password123"
        );

        updateRequest = new UpdateUserRequest(
                "John Updated",
                "john.updated@example.com",
                "newpassword123",
                "9876543210",
                Role.PATRON,
                2
        );

        userResponse = new UserResponse(
                userId,
                "John Doe",
                "john.doe@example.com",
                "encodedPassword",
                Role.PATRON,
                0
        );

        authResponse = new AuthResponse("jwt.token.here");
    }

    // Tests user registration and returns the created user.
    @Test
    void whenRegisterUser_thenReturnCreatedUser() {
        when(userService.register(any(RegisterRequest.class))).thenReturn(userResponse);
        ResponseEntity<UserResponse> response = userController.registerUser(registerRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        verify(userService).register(registerRequest);
    }

    // Tests user login and returns an authentication response.
    @Test
    void whenLogin_thenReturnAuthResponse() {
        when(userService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);
        ResponseEntity<AuthResponse> response = userController.login(authRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt.token.here");
        verify(userService).authenticate(authRequest);
    }

    // Tests getting a user by ID.
    @Test
    void whenGetUserById_thenReturnUser() {
        when(userService.getUserById(userId)).thenReturn(userResponse);
        ResponseEntity<UserResponse> response = userController.getUserById(userId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        verify(userService).getUserById(userId);
    }

    // Tests retrieving a list of all users.
    @Test
    void whenGetAllUsers_thenReturnUserList() {
        List<UserResponse> expectedUsers = Collections.singletonList(userResponse);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(expectedUsers);
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers(pageable);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(userService).getAllUsers(pageable);
    }

    // Tests updating user information and returns the updated user.
    @Test
    void whenUpdateUser_thenReturnUpdatedUser() {
        UserResponse updatedResponse = new UserResponse(
                userId,
                "John Updated",
                "john.updated@example.com",
                "encodedNewPassword",
                Role.PATRON,
                2
        );
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(updatedResponse);
        ResponseEntity<UserResponse> response = userController.updateUser(userId, updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("John Updated");
        verify(userService).updateUser(userId, updateRequest);
    }

    // Tests deleting a user and returns no content.
    @Test
    void whenDeleteUser_thenReturnNoContent() {
        doNothing().when(userService).deleteUser(userId);
        ResponseEntity<Void> response = userController.deleteUser(userId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUser(userId);
    }

    // Tests registering a user with an existing email and throws an exception.
    @Test
    void whenRegisterWithExistingEmail_thenThrowException() {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new CustomException("Email already in use", HttpStatus.CONFLICT));
        assertThatThrownBy(() -> userController.registerUser(registerRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("Email already in use");
    }

    // Tests logging in with invalid credentials and throws an exception.
    @Test
    void whenLoginWithInvalidCredentials_thenThrowException() {
        when(userService.authenticate(any(AuthRequest.class)))
                .thenThrow(new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> userController.login(authRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("Invalid credentials");
    }

    // Tests getting a user by ID when the user is not found and throws an exception.
    @Test
    void whenGetUserByIdNotFound_thenThrowException() {
        when(userService.getUserById(userId))
                .thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> userController.getUserById(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("User not found");
    }

    // Tests updating a user when the user is not found and throws an exception.
    @Test
    void whenUpdateUserNotFound_thenThrowException() {
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> userController.updateUser(userId, updateRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("User not found");
    }

    // Tests updating a librarian and throws a forbidden exception.
    @Test
    void whenUpdateLibrarian_thenThrowException() {
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenThrow(new CustomException("Librarian can't update another librarian information's!", HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> userController.updateUser(userId, updateRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("Librarian can't update another librarian information's!");
    }
}
