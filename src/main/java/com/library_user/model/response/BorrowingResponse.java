package com.library_user.model.response;

import java.time.LocalDate;
import java.util.UUID;

public record BorrowingResponse(
        UUID id,
        UUID userId,
        UUID bookId,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean overdue
) {
}
