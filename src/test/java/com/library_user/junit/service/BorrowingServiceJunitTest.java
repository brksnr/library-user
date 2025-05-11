package com.library_user.junit.service;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.OverDueReportDto;
import com.library_user.model.entity.Book;
import com.library_user.model.entity.Borrowing;
import com.library_user.model.entity.User;
import com.library_user.model.entity.Role;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.BorrowingResponse;
import com.library_user.repository.BookRepository;
import com.library_user.repository.BorrowingRepository;
import com.library_user.repository.UserRepository;
import com.library_user.service.Impl.BorrowingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceJunitTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BorrowingServiceImpl borrowingService;

    private User testUser;
    private Book testBook;
    private Borrowing testBorrowing;
    private UUID userId;
    private UUID bookId;
    private UUID borrowingId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        borrowingId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .contact("1234567890")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();

        testBook = Book.builder()
                .id(bookId)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn("978-0743273565")
                .description("A story of the fabulously wealthy Jay Gatsby")
                .publicationDate(LocalDate.of(1925, 4, 10))
                .genre("Fiction")
                .availability(true)
                .build();

        testBorrowing = Borrowing.builder()
                .id(borrowingId)
                .userId(userId)
                .bookId(bookId)
                .borrowDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .returnDate(null)
                .overdue(false)
                .build();
    }

    // Tests that a user with less than 5 books can borrow another one
    @Test
    void whenUserHasLessThan5Books_thenIsUserAvailableForBorrowShouldNotThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        borrowingService.isUserAvailableForBorrow(userId);
    }

    // Tests that a user with 5 books cannot borrow more
    @Test
    void whenUserHas5Books_thenIsUserAvailableForBorrowShouldThrowException() {
        testUser.setBorrowedBookCount(5);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> borrowingService.isUserAvailableForBorrow(userId))
                .isInstanceOf(CustomException.class);
    }

    // Tests that available books pass availability check
    @Test
    void whenBookIsAvailable_thenIsBookAvailableShouldNotThrowException() {
        when(bookRepository.existsByIdAndAvailabilityTrue(bookId)).thenReturn(true);
        borrowingService.isBookAvailable(bookId);
    }

    // Tests that unavailable books throw exception when checked
    @Test
    void whenBookIsNotAvailable_thenIsBookAvailableShouldThrowException() {
        when(bookRepository.existsByIdAndAvailabilityTrue(bookId)).thenReturn(false);
        assertThatThrownBy(() -> borrowingService.isBookAvailable(bookId))
                .isInstanceOf(CustomException.class);
    }

    // Tests that user's borrow count increases after borrowing
    @Test
    void whenIncreaseBorrowCount_thenUserBorrowCountShouldIncrease() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        borrowingService.increaseBorrowCount(userId);
        verify(userRepository).save(argThat(user -> user.getBorrowedBookCount() == 1));
    }

    @Test
    void whenChanceBookAvailability_thenBookAvailabilityShouldChange() {
        // Tests that book availability status is toggled
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        borrowingService.chanceBookAvailability(bookId);
        verify(bookRepository).save(argThat(book -> !book.isAvailability()));
    }

    // Tests successful book borrowing returns proper response
    @Test
    void whenBorrowBook_thenReturnBorrowingResponse() {
        BorrowingCreateRequest request = new BorrowingCreateRequest(userId, bookId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.existsByIdAndAvailabilityTrue(bookId)).thenReturn(true);
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(testBorrowing);

        BorrowingResponse response = borrowingService.borrowBook(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(borrowingId);
        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.userId()).isEqualTo(userId);
        verify(bookRepository).save(any(Book.class));
        verify(userRepository).save(any(User.class));
    }

    // Tests retrieving borrowing history for current user
    @Test
    void whenGetUserBorrowingHistory_thenReturnBorrowingResponseList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Borrowing> borrowingPage = new PageImpl<>(List.of(testBorrowing));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john.doe@example.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(borrowingRepository.findByUserId(userId, pageable)).thenReturn(borrowingPage);

        List<BorrowingResponse> response = borrowingService.getUserBorrowingHistory(pageable);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(borrowingId);
    }

    // Tests retrieving all borrowings in the system
    @Test
    void whenGetAllBorrowingHistory_thenReturnAllBorrowings() {
        when(borrowingRepository.findAll()).thenReturn(List.of(testBorrowing));

        List<BorrowingResponse> response = borrowingService.getAllBorrowingHistory();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(borrowingId);
    }

    // Tests retrieving overdue books report
    @Test
    void whenGetOverdueBooks_thenReturnOverdueReportList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Borrowing> borrowingPage = new PageImpl<>(List.of(testBorrowing));
        when(borrowingRepository.findByReturnDateIsNullAndDueDateBefore(any(LocalDate.class), eq(pageable)))
                .thenReturn(borrowingPage);

        List<OverDueReportDto> response = borrowingService.getOverdueBooks(pageable);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getUserId()).isEqualTo(userId);
        assertThat(response.getFirst().getBookId()).isEqualTo(bookId);
    }

    // Tests successful return of a borrowed book
    @Test
    void whenReturnBook_thenReturnBorrowingResponse() {
        ReturnBookRequest request = new ReturnBookRequest(userId);
        when(borrowingRepository.findById(borrowingId)).thenReturn(Optional.of(testBorrowing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));

        BorrowingResponse response = borrowingService.returnBook(borrowingId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(borrowingId);
        assertThat(response.returnDate()).isNotNull();
        verify(bookRepository).save(any(Book.class));
        verify(userRepository).save(any(User.class));
    }

    // Tests returning book with incorrect user throws exception
    @Test
    void whenReturnBookWithWrongUser_thenThrowException() {
        UUID wrongUserId = UUID.randomUUID();
        ReturnBookRequest request = new ReturnBookRequest(wrongUserId);
        when(borrowingRepository.findById(borrowingId)).thenReturn(Optional.of(testBorrowing));

        assertThatThrownBy(() -> borrowingService.returnBook(borrowingId, request))
                .isInstanceOf(CustomException.class);
    }

    // Tests returning a book that was already returned throws exception
    @Test
    void whenReturnAlreadyReturnedBook_thenThrowException() {
        testBorrowing.setReturnDate(LocalDate.now());
        ReturnBookRequest request = new ReturnBookRequest(userId);
        when(borrowingRepository.findById(borrowingId)).thenReturn(Optional.of(testBorrowing));

        assertThatThrownBy(() -> borrowingService.returnBook(borrowingId, request))
                .isInstanceOf(CustomException.class);
    }
}
