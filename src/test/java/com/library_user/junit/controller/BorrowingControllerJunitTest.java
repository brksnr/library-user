package com.library_user.junit.controller;

import com.library_user.controller.BorrowingController;
import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.OverDueReportDto;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import com.library_user.service.BorrowingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingControllerJunitTest {

    @Mock
    private BorrowingServiceImpl borrowingService;

    @InjectMocks
    private BorrowingController borrowingController;

    private UUID userId;
    private UUID bookId;
    private UUID borrowingId;
    private BorrowingCreateRequest borrowRequest;
    private ReturnBookRequest returnRequest;
    private BorrowingResponse borrowingResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        borrowingId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        borrowRequest = new BorrowingCreateRequest(userId, bookId);
        returnRequest = new ReturnBookRequest(userId);

        borrowingResponse = new BorrowingResponse(
                borrowingId,
                userId,
                bookId,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                null,
                false
        );
    }

    // Test the borrowing of a book
    @Test
    void whenBorrowBook_thenReturnBorrowingResponse() {
        when(borrowingService.borrowBook(any(BorrowingCreateRequest.class)))
                .thenReturn(borrowingResponse);

        ResponseEntity<BorrowingResponse> response = borrowingController.borrowBook(borrowRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(borrowingId);
        verify(borrowingService).borrowBook(borrowRequest);
    }

    // Test the returning of a book
    @Test
    void whenReturnBook_thenReturnBorrowingResponse() {
        when(borrowingService.returnBook(eq(borrowingId), any(ReturnBookRequest.class)))
                .thenReturn(borrowingResponse);

        ResponseEntity<BorrowingResponse> response = borrowingController.returnBook(borrowingId, returnRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(borrowingId);
        verify(borrowingService).returnBook(borrowingId, returnRequest);
    }

    // Test fetching the borrowing history of a user
    @Test
    void whenGetUserBorrowingHistory_thenReturnBorrowingList() {
        List<BorrowingResponse> expectedResponses = Collections.singletonList(borrowingResponse);
        when(borrowingService.getUserBorrowingHistory(any(Pageable.class)))
                .thenReturn(expectedResponses);

        ResponseEntity<List<BorrowingResponse>> response = borrowingController.getUserBorrowingHistory(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(borrowingService).getUserBorrowingHistory(pageable);
    }

    // Test fetching all borrowing histories
    @Test
    void whenGetAllBorrowingHistory_thenReturnBorrowingList() {
        List<BorrowingResponse> expectedResponses = Collections.singletonList(borrowingResponse);
        when(borrowingService.getAllBorrowingHistory()).thenReturn(expectedResponses);

        ResponseEntity<List<BorrowingResponse>> response = borrowingController.getAllBorrowingHistory();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(borrowingService).getAllBorrowingHistory();
    }

    // Test fetching overdue books report
    @Test
    void whenGetOverdueBooks_thenReturnOverdueReportList() {
        List<OverDueReportDto> expectedReports = List.of(
                new OverDueReportDto(userId, bookId, LocalDate.now(), 1)
        );
        when(borrowingService.getOverdueBooks(any(Pageable.class))).thenReturn(expectedReports);

        ResponseEntity<List<OverDueReportDto>> response = borrowingController.getOverdueBooks(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(borrowingService).getOverdueBooks(pageable);
    }

    // Test borrowing book with invalid request
    @Test
    void whenBorrowBookWithInvalidRequest_thenThrowException() {
        when(borrowingService.borrowBook(any(BorrowingCreateRequest.class)))
                .thenThrow(new CustomException("Book is not available", HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> borrowingController.borrowBook(borrowRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("Book is not available");
    }

    // Test returning book with invalid request
    @Test
    void whenReturnBookWithInvalidRequest_thenThrowException() {
        when(borrowingService.returnBook(eq(borrowingId), any(ReturnBookRequest.class)))
                .thenThrow(new CustomException("Book already returned", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> borrowingController.returnBook(borrowingId, returnRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage("Book already returned");
    }
}
