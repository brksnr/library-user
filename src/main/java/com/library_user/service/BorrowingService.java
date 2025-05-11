package com.library_user.service;

import com.library_user.model.dto.OverDueReportDto;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BorrowingService {
    void isUserAvailableForBorrow(UUID userId);

    void isBookAvailable(UUID bookId);

    void increaseBorrowCount(UUID userId);

    void chanceBookAvailability(UUID bookId);

    BorrowingResponse borrowBook(BorrowingCreateRequest request);

    List<BorrowingResponse> getUserBorrowingHistory(Pageable pageable);

    List<BorrowingResponse> getAllBorrowingHistory();

    List<OverDueReportDto> getOverdueBooks(Pageable pageable);

    BorrowingResponse returnBook(UUID borrowingId, ReturnBookRequest request);
}