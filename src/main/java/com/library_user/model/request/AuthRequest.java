package com.library_user.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AuthRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
