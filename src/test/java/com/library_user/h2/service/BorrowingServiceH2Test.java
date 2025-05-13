package com.library_user.h2.service;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.OverDueReportDto;
import com.library_user.model.entity.Book;
import com.library_user.model.entity.Borrowing;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.helper.ErrorMessages;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import com.library_user.repository.BookRepository;
import com.library_user.repository.BorrowingRepository;
import com.library_user.repository.UserRepository;
import com.library_user.service.BorrowingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BorrowingServiceH2Test {

    @Autowired
    private BorrowingService borrowingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    private User testUser;
    private User anotherUser;
    private Book availableBook;
    private Book unavailableBook;
    private Book bookForOverdueTest;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .contact("555-1234")
                .role(Role.LIBRARIAN)
                .borrowedBookCount(0)
                .build();
        testUser = userRepository.save(testUser);

        anotherUser = User.builder()
                .name("Another User")
                .email("another@example.com")
                .password("password")
                .contact("555-5678")
                .role(Role.LIBRARIAN)
                .borrowedBookCount(0)
                .build();
        anotherUser = userRepository.save(anotherUser);


        availableBook = Book.builder()
                .title("Available Book")
                .author("Author A")
                .isbn("1234567890")
                .description("This book is available for borrowing.")
                .publicationDate(LocalDate.of(2022, 1, 1))
                .genre("Fiction")
                .availability(true)
                .build();
        availableBook = bookRepository.save(availableBook);

        unavailableBook = Book.builder()
                .title("Unavailable Book")
                .author("Author B")
                .isbn("1234567891")
                .description("This book is currently unavailable.")
                .publicationDate(LocalDate.of(2022, 1, 1))
                .genre("Science")
                .availability(false)
                .build();
        unavailableBook = bookRepository.save(unavailableBook);

        bookForOverdueTest = Book.builder()
                .title("Book for Overdue Test")
                .author("Author O")
                .isbn("0000000000")
                .description("A book to be used in overdue tests.")
                .publicationDate(LocalDate.of(2020, 1, 1))
                .genre("Mystery")
                .availability(true)
                .build();
        bookForOverdueTest = bookRepository.save(bookForOverdueTest);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Successful book borrowing when book is available and user can borrow.
    @Test
    void borrowBook_whenBookIsAvailableAndUserCanBorrow_shouldSucceed() {
        BorrowingCreateRequest request = new BorrowingCreateRequest(testUser.getId(), availableBook.getId());

        BorrowingResponse response = borrowingService.borrowBook(request);

        assertNotNull(response);
        assertEquals(testUser.getId(), response.userId());
        assertEquals(availableBook.getId(), response.bookId());
        assertEquals(LocalDate.now(), response.borrowDate());
        assertEquals(LocalDate.now().plusDays(1), response.dueDate());
        assertFalse(response.overdue());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getBorrowedBookCount());

        Book updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertFalse(updatedBook.isAvailability());

        assertTrue(borrowingRepository.existsById(response.id()));
    }

    //  borrowing an unavailable book throws a CustomException.
    @Test
    void borrowBook_whenBookIsNotAvailable_shouldThrowCustomException() {
        BorrowingCreateRequest request = new BorrowingCreateRequest(testUser.getId(), unavailableBook.getId());

        CustomException exception = assertThrows(CustomException.class, () -> borrowingService.borrowBook(request));
        assertEquals(ErrorMessages.BOOK_NOT_AVAILABLE, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    //  borrowing a book when the user has reached the maximum limit throws a CustomException.
    @Test
    void borrowBook_whenUserReachedMaxBorrowLimit_shouldThrowCustomException() {
        testUser.setBorrowedBookCount(5);
        userRepository.save(testUser);

        BorrowingCreateRequest request = new BorrowingCreateRequest(testUser.getId(), availableBook.getId());

        CustomException exception = assertThrows(CustomException.class, () -> borrowingService.borrowBook(request));
        assertEquals(ErrorMessages.USER_CAN_NOT_BORROW_5, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    }

    // retrieving a user's borrowing history when they have existing borrowings.
    @Test
    void getUserBorrowingHistory_whenUserHasBorrowings_shouldReturnHistory() {
        mockSecurityContext(testUser);

        borrowingService.borrowBook(new BorrowingCreateRequest(testUser.getId(), availableBook.getId()));

        Pageable pageable = PageRequest.of(0, 10);
        List<BorrowingResponse> history = borrowingService.getUserBorrowingHistory(pageable);

        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
        assertEquals(testUser.getId(), history.getFirst().userId());
        assertEquals(availableBook.getId(), history.getFirst().bookId());
    }

    // retrieving a user's borrowing history when they have no borrowings, expecting an empty list.
    @Test
    void getUserBorrowingHistory_whenUserHasNoBorrowings_shouldReturnEmptyList() {
        mockSecurityContext(testUser);

        Pageable pageable = PageRequest.of(0, 10);
        List<BorrowingResponse> history = borrowingService.getUserBorrowingHistory(pageable);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
    // retrieving borrowing history for a non-existent user throws a CustomException.
    @Test
    void getUserBorrowingHistory_whenUserNotFound_shouldThrowCustomException() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("nonexistent@example.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Pageable pageable = PageRequest.of(0, 10);
        CustomException exception = assertThrows(CustomException.class, () -> {
            borrowingService.getUserBorrowingHistory(pageable);
        });
        assertEquals(ErrorMessages.USER_NOT_FOUND_EMAIL, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }


    // retrieving all borrowing history records from the system.
    @Test
    void getAllBorrowingHistory_shouldReturnAllBorrowings() {
        BorrowingResponse b1 = borrowingService.borrowBook(new BorrowingCreateRequest(testUser.getId(), availableBook.getId()));

        Book anotherAvailableBook = Book.builder()
                .title("Another Available Book for History")
                .author("Author C")
                .isbn("7890123456")
                .description("Another test book description for all history.")
                .publicationDate(LocalDate.of(2021, 5, 20))
                .genre("History") // Added genre
                .availability(true)
                .build();
        anotherAvailableBook = bookRepository.save(anotherAvailableBook);
        BorrowingResponse b2 = borrowingService.borrowBook(new BorrowingCreateRequest(anotherUser.getId(), anotherAvailableBook.getId()));

        List<BorrowingResponse> allHistory = borrowingService.getAllBorrowingHistory();

        assertNotNull(allHistory);
        assertEquals(2, allHistory.size());
        assertTrue(allHistory.stream().anyMatch(b -> b.id().equals(b1.id())));
        assertTrue(allHistory.stream().anyMatch(b -> b.id().equals(b2.id())));
    }

    // retrieving only overdue and not-returned books in the overdue report.
    @Test
    void getOverdueBooks_shouldReturnOnlyOverdueAndNotReturnedBooks() {
        LocalDate today = LocalDate.now();

        Book bookForOverdue = bookRepository.findById(bookForOverdueTest.getId()).orElseThrow();
        bookForOverdue.setAvailability(false);
        bookRepository.save(bookForOverdue);

        Borrowing overdueBorrowing = Borrowing.builder()
                .userId(testUser.getId())
                .bookId(bookForOverdue.getId())
                .borrowDate(today.minusDays(10))
                .dueDate(today.minusDays(5))
                .returnDate(null)
                .overdue(false)
                .build();
        borrowingRepository.save(overdueBorrowing);

        Book book2 = Book.builder()
                .title("Book 2 Not Overdue")
                .author("Author D")
                .isbn("b2isbn")
                .description("Borrowed book still not overdue")
                .publicationDate(LocalDate.of(2021, 6, 15))
                .genre("Technology") // Added genre
                .availability(false)
                .build();
        bookRepository.save(book2);
        Borrowing notOverdueBorrowing = Borrowing.builder()
                .userId(testUser.getId())
                .bookId(book2.getId())
                .borrowDate(today.minusDays(1))
                .dueDate(today.plusDays(5))
                .returnDate(null)
                .overdue(false)
                .build();
        borrowingRepository.save(notOverdueBorrowing);

        Book book3 = Book.builder()
                .title("Book 3 Overdue Returned")
                .author("Author E")
                .isbn("b3isbn")
                .description("Returned overdue book")
                .publicationDate(LocalDate.of(2020, 3, 10))
                .genre("Biography") // Added genre
                .availability(true)
                .build();
        bookRepository.save(book3);
        Borrowing overdueButReturnedBorrowing = Borrowing.builder()
                .userId(testUser.getId())
                .bookId(book3.getId())
                .borrowDate(today.minusDays(10))
                .dueDate(today.minusDays(5))
                .returnDate(today.minusDays(1))
                .overdue(true)
                .build();
        borrowingRepository.save(overdueButReturnedBorrowing);


        Pageable pageable = PageRequest.of(0, 10);
        List<OverDueReportDto> overdueBooksReport = borrowingService.getOverdueBooks(pageable);

        assertNotNull(overdueBooksReport);
        assertEquals(1, overdueBooksReport.size(), "Should only find one overdue and not-returned book.");
        OverDueReportDto report = overdueBooksReport.getFirst();
        assertEquals(overdueBorrowing.getUserId(), report.getUserId());
        assertEquals(overdueBorrowing.getBookId(), report.getBookId());
        assertEquals(overdueBorrowing.getDueDate(), report.getDueDate());
        assertEquals(5, report.getDaysOverdue(), "Days overdue calculation.");
    }

    // successful book return when the borrowing exists and has not been returned.
    @Test
    void returnBook_whenBorrowingExistsAndNotReturned_shouldSucceed() {
        BorrowingResponse borrowed = borrowingService.borrowBook(new BorrowingCreateRequest(testUser.getId(), availableBook.getId()));
        UUID borrowingId = borrowed.id();

        Book bookAfterBorrow = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertFalse(bookAfterBorrow.isAvailability(), "Book should be unavailable after borrowing.");
        User userAfterBorrow = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, userAfterBorrow.getBorrowedBookCount(), "User borrow count should be 1.");


        ReturnBookRequest returnRequest = new ReturnBookRequest(testUser.getId());
        BorrowingResponse returnedResponse = borrowingService.returnBook(borrowingId, returnRequest);

        assertNotNull(returnedResponse);
        assertEquals(borrowingId, returnedResponse.id());
        assertEquals(testUser.getId(), returnedResponse.userId());
        assertEquals(availableBook.getId(), returnedResponse.bookId());
        assertNotNull(returnedResponse.returnDate());
        assertEquals(LocalDate.now(), returnedResponse.returnDate());
        assertFalse(returnedResponse.overdue(), "Should not be overdue if returned on or before due date.");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getBorrowedBookCount(), "Borrowed book count should decrease.");

        Book updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertTrue(updatedBook.isAvailability(), "Book should be available again after return.");

        Borrowing updatedBorrowing = borrowingRepository.findById(borrowingId).orElseThrow();
        assertNotNull(updatedBorrowing.getReturnDate());
    }

    // returning a book that is overdue marks the borrowing record as overdue.
    @Test
    void returnBook_whenBookIsOverdue_shouldMarkAsOverdue() {
        // Ensure availableBook is set up for this specific scenario
        Book currentAvailableBookState = bookRepository.findById(availableBook.getId()).orElseThrow();
        currentAvailableBookState.setAvailability(false); // Manually set for this direct borrowing creation
        bookRepository.save(currentAvailableBookState);

        User currentUserState = userRepository.findById(testUser.getId()).orElseThrow();
        currentUserState.setBorrowedBookCount(1); // Manually set as well
        userRepository.save(currentUserState);

        LocalDate borrowDate = LocalDate.now().minusDays(10);
        LocalDate dueDate = LocalDate.now().minusDays(5);

        Borrowing borrowing = Borrowing.builder()
                .userId(testUser.getId())
                .bookId(availableBook.getId())
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .returnDate(null)
                .overdue(false)
                .build();
        borrowing = borrowingRepository.save(borrowing);

        ReturnBookRequest returnRequest = new ReturnBookRequest(testUser.getId());
        BorrowingResponse response = borrowingService.returnBook(borrowing.getId(), returnRequest);

        assertNotNull(response);
        assertEquals(borrowing.getId(), response.id());
        assertTrue(response.overdue(), "Response should indicate book was overdue.");
        assertEquals(LocalDate.now(), response.returnDate());

        Borrowing updatedBorrowingInDb = borrowingRepository.findById(borrowing.getId()).orElseThrow();
        assertTrue(updatedBorrowingInDb.isOverdue(), "Borrowing entity in DB should be marked as overdue.");
        assertNotNull(updatedBorrowingInDb.getReturnDate());

        Book updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertTrue(updatedBook.isAvailability(), "Book should become available after return.");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getBorrowedBookCount(), "User's borrowed book count should be decremented.");
    }


    // attempting to return a book with a non-existent borrowing ID throws a CustomException.
    @Test
    void returnBook_whenBorrowingNotFound_shouldThrowCustomException() {
        UUID nonExistentBorrowingId = UUID.randomUUID();
        ReturnBookRequest returnRequest = new ReturnBookRequest(testUser.getId());

        CustomException exception = assertThrows(CustomException.class,
                () -> borrowingService.returnBook(nonExistentBorrowingId, returnRequest));
        assertEquals(ErrorMessages.BORROWING_NOT_FOUND, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    // attempting to return a book that has already been returned throws a CustomException.
    @Test
    void returnBook_whenBookAlreadyReturned_shouldThrowCustomException() {
        BorrowingResponse borrowed = borrowingService.borrowBook(new BorrowingCreateRequest(testUser.getId(), availableBook.getId()));
        borrowingService.returnBook(borrowed.id(), new ReturnBookRequest(testUser.getId()));

        ReturnBookRequest secondReturnRequest = new ReturnBookRequest(testUser.getId());
        CustomException exception = assertThrows(CustomException.class,
                () -> borrowingService.returnBook(borrowed.id(), secondReturnRequest));
        assertEquals(ErrorMessages.BOOK_ALREADY_RETURNED, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    }

    // attempting to return a book with a user ID that does not match the borrowing record throws a CustomException.
    @Test
    void returnBook_whenUserIdDoesNotMatchBorrowing_shouldThrowCustomException() {
        BorrowingResponse borrowed = borrowingService.borrowBook(new BorrowingCreateRequest(testUser.getId(), availableBook.getId()));

        ReturnBookRequest returnRequestWithWrongUser = new ReturnBookRequest(anotherUser.getId());

        CustomException exception = assertThrows(CustomException.class,
                () -> borrowingService.returnBook(borrowed.id(), returnRequestWithWrongUser));
        assertEquals(ErrorMessages.USER_AND_BORROW_ID_NOT_MATCH, exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    }

    // checking user availability for borrowing does not throw an exception when the user is below the max limit.
    @Test
    void isUserAvailableForBorrow_whenUserHasLessThanMax_shouldNotThrow() {
        testUser.setBorrowedBookCount(4);
        userRepository.save(testUser);
        assertDoesNotThrow(() -> borrowingService.isUserAvailableForBorrow(testUser.getId()));
    }

    // checking user availability for borrowing throws a CustomException when the user is at the maximum limit.
    @Test
    void isUserAvailableForBorrow_whenUserHasMax_shouldThrow() {
        testUser.setBorrowedBookCount(5);
        userRepository.save(testUser);
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.isUserAvailableForBorrow(testUser.getId()));
        assertEquals(ErrorMessages.USER_CAN_NOT_BORROW_5, ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    // checking user availability for a non-existent user throws a CustomException.
    @Test
    void isUserAvailableForBorrow_whenUserNotFound_shouldThrow() {
        UUID nonExistentUserId = UUID.randomUUID();
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.isUserAvailableForBorrow(nonExistentUserId));
        assertEquals(ErrorMessages.USER_NOT_FOUND_ID, ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }


    // checking book availability does not throw an exception when the book is available.
    @Test
    void isBookAvailable_whenBookIsAvailable_shouldNotThrow() {
        assertTrue(availableBook.isAvailability());
        assertDoesNotThrow(() -> borrowingService.isBookAvailable(availableBook.getId()));
    }

    // checking book availability throws a CustomException when the book is not available.
    @Test
    void isBookAvailable_whenBookIsNotAvailable_shouldThrow() {
        assertFalse(unavailableBook.isAvailability());
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.isBookAvailable(unavailableBook.getId()));
        assertEquals(ErrorMessages.BOOK_NOT_AVAILABLE, ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    // checking book availability for a non-existent book throws a CustomException.
    @Test
    void isBookAvailable_whenBookNotFound_shouldThrow() {
        UUID nonExistentBookId = UUID.randomUUID();
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.isBookAvailable(nonExistentBookId));
        assertEquals(ErrorMessages.BOOK_NOT_AVAILABLE, ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    // increasing a user's borrowed book count is successful.
    @Test
    void increaseBorrowCount_shouldIncreaseUserBorrowCount() {
        int initialCount = testUser.getBorrowedBookCount();
        borrowingService.increaseBorrowCount(testUser.getId());
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(initialCount + 1, updatedUser.getBorrowedBookCount());
    }

    // increasing borrow count for a non-existent user throws a CustomException.
    @Test
    void increaseBorrowCount_whenUserNotFound_shouldThrow() {
        UUID nonExistentUserId = UUID.randomUUID();
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.increaseBorrowCount(nonExistentUserId));
        assertEquals(ErrorMessages.USER_NOT_FOUND_ID, ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    // changing book availability successfully toggles the availability status.
    @Test
    void chanceBookAvailability_shouldToggleBookAvailability() {
        boolean initialAvailability = availableBook.isAvailability();
        borrowingService.chanceBookAvailability(availableBook.getId());
        Book updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertEquals(!initialAvailability, updatedBook.isAvailability());

        borrowingService.chanceBookAvailability(availableBook.getId());
        updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertEquals(initialAvailability, updatedBook.isAvailability());
    }

    // changing availability for a non-existent book throws a CustomException.
    @Test
    void chanceBookAvailability_whenBookNotFound_shouldThrow() {
        UUID nonExistentBookId = UUID.randomUUID();
        CustomException ex = assertThrows(CustomException.class, () -> borrowingService.chanceBookAvailability(nonExistentBookId));
        assertEquals(ErrorMessages.BOOK_NOT_FOUND_ID, ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }
}