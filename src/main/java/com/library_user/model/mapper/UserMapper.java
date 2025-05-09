package com.library_user.model.mapper;

import com.library_user.model.dto.UserDto;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;

public class UserMapper {
    public static UserDto toDto(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .contact(user.getContact())
                .role(user.getRole())
                .borrowedBookCount(user.getRole() == Role.PATRON ? user.getBorrowedBookCount() : null)
                .build();
    }
}
