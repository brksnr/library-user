package com.library_user.controller;

import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import com.library_user.model.dto.OverDueReportDto;
import com.library_user.service.BorrowingService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
@Tag(name = "Borrowing", description = "Borrowing and Returning API")
public class BorrowingController {

    private final BorrowingService borrowingService;

    @Operation(
            summary = "Borrow a book",
            description = "Patrons can borrow an available book. Requires userId and bookId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book borrowed successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BorrowingResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "User or book not found"),
                    @ApiResponse(responseCode = "409", description = "Book is not available or user has reached the borrowing limit")
            }
    )
    @PostMapping("/borrow")
    @PreAuthorize("hasRole('PATRON')")
    public ResponseEntity<BorrowingResponse> borrowBook(@Valid @RequestBody BorrowingCreateRequest request) {
        return ResponseEntity.ok(borrowingService.borrowBook(request));
    }

    @Operation(
            summary = "Return a book",
            description = "Patrons can return a borrowed book.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book returned successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BorrowingResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Borrowing record not found"),
                    @ApiResponse(responseCode = "409", description = "Book already returned")
            }
    )
    @PostMapping("/return/{borrowingId}")
    @PreAuthorize("hasRole('PATRON')")
    public ResponseEntity<BorrowingResponse> returnBook(@PathVariable UUID borrowingId, @RequestBody ReturnBookRequest request) {
        return ResponseEntity.ok(borrowingService.returnBook(borrowingId , request));
    }



    @Operation(
            summary = "Get user borrowing history",
            description = "Get borrowing history for a user (paginated).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Borrowing history retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BorrowingResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/history/user/{page}/{size}")
    @PreAuthorize("hasRole('PATRON') or hasRole('LIBRARIAN')")
    public ResponseEntity<List<BorrowingResponse>> getUserBorrowingHistory(
            @PathVariable int page,
            @PathVariable int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(borrowingService.getUserBorrowingHistory(pageable));
    }

    @Operation(
            summary = "Get all borrowing history",
            description = "Librarian gets all users' borrowing history.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All borrowing history retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BorrowingResponse.class)))
            }
    )
    @GetMapping("/history/all")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<BorrowingResponse>> getAllBorrowingHistory() {
        return ResponseEntity.ok(borrowingService.getAllBorrowingHistory());
    }

    @Operation(
            summary = "Get overdue books",
            description = "Librarian gets report of overdue books (paginated).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Overdue books report retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OverDueReportDto.class)))
            }
    )
    @GetMapping("/overdue/page/{page}/size/{size}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<OverDueReportDto>> getOverdueBooks(
            @PathVariable int page,
            @PathVariable int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(borrowingService.getOverdueBooks(pageable));
    }
}