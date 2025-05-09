package com.library_user.model.response;

import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String password,
        String contact,
        Role role,
        Integer borrowedBookCount
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getContact(),
                user.getRole(),
                user.getBorrowedBookCount()
        );
    }
}
