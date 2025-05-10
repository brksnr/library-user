package com.library_user.model.mapper;

import com.library_user.model.entity.Borrowing;
import com.library_user.model.response.BorrowingResponse;

public class BorrowingMapper {

    public static BorrowingResponse toResponseDTO(Borrowing borrowing) {
        return new BorrowingResponse(
                borrowing.getId(),
                borrowing.getUserId(),
                borrowing.getBookId(),
                borrowing.getBorrowDate(),
                borrowing.getDueDate(),
                borrowing.getReturnDate(),
                borrowing.isOverdue()
        );
    }
}
