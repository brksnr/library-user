package com.library_user.model.request;

import com.library_user.model.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String contact,
        @NotBlank Role role,
        @NotBlank Integer borrowedBookCount
) {
}
