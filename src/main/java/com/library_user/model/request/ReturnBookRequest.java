package com.library_user.model.request;

import java.util.UUID;

public record ReturnBookRequest(
        UUID userId
) {
}
