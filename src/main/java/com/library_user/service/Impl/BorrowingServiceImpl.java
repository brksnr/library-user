package com.library_user.service.Impl;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.OverDueReportDto;
import com.library_user.model.entity.Book;
import com.library_user.model.entity.Borrowing;
import com.library_user.model.entity.User;
import com.library_user.helper.ErrorMessages;
import com.library_user.model.mapper.BorrowingMapper;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import com.library_user.repository.BookRepository;
import com.library_user.repository.BorrowingRepository;
import com.library_user.repository.UserRepository;
import com.library_user.service.BorrowingService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BorrowingServiceImpl implements BorrowingService {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    @Autowired
    public BorrowingServiceImpl(UserRepository userRepository, BookRepository bookRepository, BorrowingRepository borrowingRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
    }

    /**
     Checks if the user is allowed to borrow more books (max 5)
     * */
    @Override
    public void isUserAvailableForBorrow(UUID userId){
        int MAX_BORROWED_BOOKS = 5;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorMessages.USER_NOT_FOUND_ID, HttpStatus.NOT_FOUND));

        if(user.getBorrowedBookCount() >= MAX_BORROWED_BOOKS){
            throw new CustomException(ErrorMessages.USER_CAN_NOT_BORROW_5,HttpStatus.CONFLICT);
        }
    }

    /**
     Checks if the requested book is currently available for borrowing
     */
    @Override
    public void isBookAvailable(UUID bookId){
        boolean isBookAvailable = bookRepository.existsByIdAndAvailabilityTrue(bookId);
        if (!isBookAvailable) {
            throw new CustomException(ErrorMessages.BOOK_NOT_AVAILABLE, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     Increases the user's borrowed book count by one
     * */
    @Override
    public void increaseBorrowCount(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorMessages.USER_NOT_FOUND_ID,HttpStatus.NOT_FOUND));
        user.setBorrowedBookCount(user.getBorrowedBookCount() + 1);
        userRepository.save(user);
    }

    /**
     Toggles the availability status of a book
     * */
    @Override
    public void chanceBookAvailability(UUID bookId){
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorMessages.BOOK_NOT_FOUND_ID, HttpStatus.NOT_FOUND));
        book.setAvailability(!book.isAvailability());
        bookRepository.save(book);
    }

    /**
     Handles the borrowing process and returns borrowing information
     */
    @Override
    public BorrowingResponse borrowBook(BorrowingCreateRequest request) {
        isBookAvailable(request.bookId());
        isUserAvailableForBorrow(request.userId());

        LocalDate now = LocalDate.now();
        Borrowing borrowing = Borrowing.builder()
                .userId(request.userId())
                .bookId(request.bookId())
                .borrowDate(now)
                .dueDate(now.plusDays(1))
                .overdue(false)
                .build();
        borrowing = borrowingRepository.save(borrowing);
        increaseBorrowCount(request.userId());
        chanceBookAvailability(request.bookId());
        return BorrowingMapper.toResponseDTO(borrowing);
    }

    /**
     Retrieves the authenticated user's borrowing history with pagination
     * */
    @Override
    public List<BorrowingResponse> getUserBorrowingHistory(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorMessages.USER_NOT_FOUND_EMAIL, HttpStatus.NOT_FOUND));

        return borrowingRepository.findByUserId(user.getId(), pageable)
                .map(BorrowingMapper::toResponseDTO)
                .getContent();
    }

    /**
     Retrieves the borrowing history of all users
     **/
    @Override
    public List<BorrowingResponse> getAllBorrowingHistory() {
        return borrowingRepository.findAll().stream()
                .map(BorrowingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     Returns a list of overdue books and related information
     **/
    @Override
    public List<OverDueReportDto> getOverdueBooks(Pageable pageable) {
        LocalDate today = LocalDate.now();
        return borrowingRepository.findByReturnDateIsNullAndDueDateBefore(today, pageable)
                .map(borrowing -> new OverDueReportDto(
                        borrowing.getUserId(),
                        borrowing.getBookId(),
                        borrowing.getDueDate(),
                        java.time.temporal.ChronoUnit.DAYS.between(borrowing.getDueDate(), today)
                ))
                .getContent();
    }


    /**
     Handles the return process of a borrowed book
     * */
        @Transactional
        @Override
        public BorrowingResponse returnBook(UUID borrowingId, ReturnBookRequest request) {
            Borrowing borrowing = getBorrowingOrThrow(borrowingId);
            checkAlreadyReturned(borrowing);


            if (!borrowing.getUserId().equals(request.userId())) {
                throw new CustomException(ErrorMessages.USER_AND_BORROW_ID_NOT_MATCH, HttpStatus.FORBIDDEN);
            }

            updateBorrowingReturnInfo(borrowing);
            updateBookAvailability(borrowing.getBookId(), true);
            updateUserBorrowedBookCount(borrowing.getUserId());

            return BorrowingMapper.toResponseDTO(borrowing);
        }


    /**
     Fetches a borrowing record or throws an exception if not found
     * */
    private Borrowing getBorrowingOrThrow(UUID borrowingId) {
        return borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new CustomException(ErrorMessages.BORROWING_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    /**
     Checks if the book has already been returned
     * */
    private void checkAlreadyReturned(Borrowing borrowing) {
        if (borrowing.getReturnDate() != null) {
            throw new CustomException(ErrorMessages.BOOK_ALREADY_RETURNED, HttpStatus.CONFLICT);
        }
    }

    /**
     Updates the return date and overdue status of a borrowing
     * */
    private void updateBorrowingReturnInfo(Borrowing borrowing) {
        LocalDate now = LocalDate.now();
        borrowing.setReturnDate(now);
        borrowing.setOverdue(now.isAfter(borrowing.getDueDate()));
        borrowingRepository.save(borrowing);
    }

    /**
     Sets the availability of a book to true or false
     * */
    private void updateBookAvailability(UUID bookId, boolean available) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorMessages.BOOK_NOT_FOUND_ID, HttpStatus.NOT_FOUND));
        book.setAvailability(available);
        bookRepository.save(book);
    }

    /**
     Adjusts the user's borrowed book count
     */
    private void updateUserBorrowedBookCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorMessages.USER_NOT_FOUND_ID, HttpStatus.NOT_FOUND));
        user.setBorrowedBookCount(user.getBorrowedBookCount() + -1);
        userRepository.save(user);
    }


}
