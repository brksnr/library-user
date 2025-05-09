package com.library_user.service;

import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {
    AuthResponse authenticate(AuthRequest request);
    UserResponse register(RegisterRequest request);
    UserResponse getUserById(UUID id);
    List<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
}