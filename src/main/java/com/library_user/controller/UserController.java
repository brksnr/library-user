package com.library_user.controller;

import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import com.library_user.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "User Management", description = "User registration, authentication and management operations")
public class UserController {

    private final UserServiceImpl userServiceImpl;
    @Autowired
    public UserController(UserServiceImpl userServiceImpl) { this.userServiceImpl = userServiceImpl; }

    @Operation(
            summary = "Register a new user",
            description = "Registers a new user (patron or librarian) with the provided information.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Email already in use"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping(
            value = "/register",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = userServiceImpl.register(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Authenticate user and get JWT token",
            description = "Authenticates the user and returns a JWT token if credentials are valid.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials")
            }
    )
    @PostMapping(
            value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = userServiceImpl.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves user details by user ID. Only librarians can access this endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = userServiceImpl.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users. Only librarians can access this endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class)))
            }
    )
    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(Pageable pageable) {
        List<UserResponse> users = userServiceImpl.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Update user information",
            description = "Updates user information by user ID. Only librarians can update users.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "Email already in use")
            }
    )
    @PutMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        UserResponse updated = userServiceImpl.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes a user by user ID. Only librarians can delete users.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @DeleteMapping(
            value = "/{id}"
    )
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}