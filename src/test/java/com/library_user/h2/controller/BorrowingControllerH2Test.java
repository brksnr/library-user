package com.library_user.h2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library_user.model.entity.Book;
import com.library_user.model.entity.Borrowing;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.BorrowingCreateRequest;
import com.library_user.model.request.ReturnBookRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.repository.BookRepository;
import com.library_user.repository.BorrowingRepository;
import com.library_user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BorrowingControllerH2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String librarianToken;
    private String patronToken;

    private User librarianUser;
    private User patronUser;
    private User anotherPatron;

    private Book testBook1;
    private Book testBook2;
    private Book unavailableBook;
    private Book borrowedBook;

    private static final int DUE_PERIOD_DAYS = 14;


    @BeforeEach
    void setUp() throws Exception {
        objectMapper.findAndRegisterModules();

        borrowingRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();


        librarianUser = User.builder()
                .name("Librarian ForBorrowCtrl")
                .email("librarian.borrowctrl@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("1234567001")
                .role(Role.LIBRARIAN)
                .borrowedBookCount(0)
                .build();
        userRepository.save(librarianUser);

        patronUser = User.builder()
                .name("Patron ForBorrowCtrl")
                .email("patron.borrowctrl@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("0987654001")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();
        userRepository.save(patronUser);

        anotherPatron = User.builder()
                .name("Another Patron")
                .email("another.patron@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("1122334455")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();
        userRepository.save(anotherPatron);


        // Create Books
        testBook1 = Book.builder()
                .title("Borrowable Book Alpha")
                .author("Author Alpha")
                .isbn("111000111A")
                .description("A book available for borrowing by Alpha patron.")
                .publicationDate(LocalDate.now().minusYears(1))
                .genre("Fiction")
                .availability(true)
                .build();
        bookRepository.save(testBook1);

        testBook2 = Book.builder()
                .title("Borrowable Book Beta")
                .author("Author Beta")
                .isbn("222000222B")
                .description("Another book available for borrowing.")
                .publicationDate(LocalDate.now().minusMonths(6))
                .genre("Science")
                .availability(true)
                .build();
        bookRepository.save(testBook2);

        unavailableBook = Book.builder()
                .title("Unavailable Book Gamma")
                .author("Author Gamma")
                .isbn("333000333G")
                .description("A book that is not available for borrowing.")
                .publicationDate(LocalDate.now().minusYears(2))
                .genre("History")
                .availability(false)
                .build();
        bookRepository.save(unavailableBook);

        borrowedBook = Book.builder()
                .title("Already Borrowed Book Delta")
                .author("Author Delta")
                .isbn("444000444D")
                .description("A book that will be pre-borrowed for return tests.")
                .publicationDate(LocalDate.now().minusMonths(3))
                .genre("Mystery")
                .availability(false)
                .build();
        bookRepository.save(borrowedBook);


        // Authenticate users and get tokens
        librarianToken = authenticateAndGetToken(librarianUser.getEmail(), "password123");
        patronToken = authenticateAndGetToken(patronUser.getEmail(), "password123");
    }

    @AfterEach
    void tearDown() {
        borrowingRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private String authenticateAndGetToken(String email, String password) throws Exception {
        AuthRequest authRequest = AuthRequest.builder().email(email).password(password).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.token();
    }

    // successful book borrowing by a patron user.
    @Test
    void borrowBook_asPatron_success() throws Exception {
        BorrowingCreateRequest request = new BorrowingCreateRequest(patronUser.getId(), testBook1.getId());

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(patronUser.getId().toString())))
                .andExpect(jsonPath("$.bookId", is(testBook1.getId().toString())))
                .andExpect(jsonPath("$.borrowDate", is(LocalDate.now().toString())))
                .andExpect(jsonPath("$.dueDate", is(LocalDate.now().plusDays(1).toString()))) // Assuming default due period is 1 day in service for testing
                .andExpect(jsonPath("$.returnDate").doesNotExist())
                .andExpect(jsonPath("$.overdue", is(false)));

        Book updatedBook = bookRepository.findById(testBook1.getId()).orElseThrow();
        assertFalse(updatedBook.isAvailability());
        User updatedUser = userRepository.findById(patronUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getBorrowedBookCount());
    }

    // a librarian attempting to borrow a book is forbidden.
    @Test
    void borrowBook_asLibrarian_forbidden() throws Exception {
        BorrowingCreateRequest request = new BorrowingCreateRequest(librarianUser.getId(), testBook1.getId());

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // borrowing a book that is not available returns a bad request error.
    @Test
    void borrowBook_bookNotAvailable_badRequest() throws Exception {
        BorrowingCreateRequest request = new BorrowingCreateRequest(patronUser.getId(), unavailableBook.getId());

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // borrowing a book with a non-existent user ID in the request returns a not found error.
    @Test
    void borrowBook_userNotFoundInRequest_notFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        BorrowingCreateRequest request = new BorrowingCreateRequest(nonExistentUserId, testBook1.getId());

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // borrowing a non-existent book returns a bad request error.
    @Test
    void borrowBook_bookNotFound_badRequest() throws Exception {
        UUID nonExistentBookId = UUID.randomUUID();
        BorrowingCreateRequest request = new BorrowingCreateRequest(patronUser.getId(), nonExistentBookId);

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // borrowing a book when the user has reached the maximum borrow limit returns a conflict error.
    @Test
    void borrowBook_userReachedBorrowLimit_conflict() throws Exception {
        patronUser.setBorrowedBookCount(5); // Set user to max limit
        userRepository.save(patronUser);

        BorrowingCreateRequest request = new BorrowingCreateRequest(patronUser.getId(), testBook2.getId());

        mockMvc.perform(post("/api/borrowings/borrow")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private Borrowing createSampleBorrowing(User user, Book book, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, boolean overdue) {
        Borrowing borrowing = Borrowing.builder()
                .userId(user.getId())
                .bookId(book.getId())
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .returnDate(returnDate)
                .overdue(overdue)
                .build();
        return borrowingRepository.save(borrowing);
    }

    // successful book return by a patron user.
    @Test
    void returnBook_asPatron_success() throws Exception {
        // Setup for a book that is currently borrowed
        borrowedBook.setAvailability(false);
        bookRepository.save(borrowedBook);
        patronUser.setBorrowedBookCount(1);
        userRepository.save(patronUser);

        // Create a borrowing record that is not yet returned
        Borrowing borrowingRecord = createSampleBorrowing(patronUser, borrowedBook, LocalDate.now().minusDays(5), LocalDate.now().plusDays(DUE_PERIOD_DAYS - 5), null, false);
        ReturnBookRequest returnRequest = new ReturnBookRequest(patronUser.getId());

        mockMvc.perform(post("/api/borrowings/return/{borrowingId}", borrowingRecord.getId())
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(borrowingRecord.getId().toString())))
                .andExpect(jsonPath("$.returnDate", is(LocalDate.now().toString())))
                .andExpect(jsonPath("$.overdue", is(false))); // Assuming it's not overdue in this scenario

        // Verify book availability and user borrow count are updated
        Book updatedBook = bookRepository.findById(borrowedBook.getId()).orElseThrow();
        assertTrue(updatedBook.isAvailability());
        User updatedUser = userRepository.findById(patronUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getBorrowedBookCount());
    }

    // a librarian attempting to return a book is forbidden.
    @Test
    void returnBook_asLibrarian_forbidden() throws Exception {
        // Create a sample borrowing record for a patron
        Borrowing borrowingRecord = createSampleBorrowing(patronUser, borrowedBook, LocalDate.now().minusDays(5), LocalDate.now().plusDays(DUE_PERIOD_DAYS - 5), null, false);
        ReturnBookRequest returnRequest = new ReturnBookRequest(patronUser.getId());

        // Attempt to return as a librarian
        mockMvc.perform(post("/api/borrowings/return/{borrowingId}", borrowingRecord.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isForbidden());
    }

    // attempting to return a book with a non-existent borrowing record ID returns a not found error.
    @Test
    void returnBook_borrowingRecordNotFound_notFound() throws Exception {
        UUID nonExistentBorrowingId = UUID.randomUUID(); // Non-existent ID
        ReturnBookRequest returnRequest = new ReturnBookRequest(patronUser.getId());

        mockMvc.perform(post("/api/borrowings/return/{borrowingId}", nonExistentBorrowingId)
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isNotFound());
    }

    // attempting to return a book that has already been returned returns a conflict error.
    @Test
    void returnBook_alreadyReturned_conflict() throws Exception {
        // Create a borrowing record that is already marked as returned
        Borrowing borrowingRecord = createSampleBorrowing(patronUser, borrowedBook, LocalDate.now().minusDays(10), LocalDate.now().minusDays(10 - DUE_PERIOD_DAYS), LocalDate.now().minusDays(2), false);
        ReturnBookRequest returnRequest = new ReturnBookRequest(patronUser.getId());

        // Attempt to return it again
        mockMvc.perform(post("/api/borrowings/return/{borrowingId}", borrowingRecord.getId())
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isConflict());
    }


    // retrieving a patron's own borrowing history.
    @Test
    void getUserBorrowingHistory_asPatron_success() throws Exception {
        // Create some borrowing records for the patron
        createSampleBorrowing(patronUser, testBook1, LocalDate.now().minusDays(10), LocalDate.now().minusDays(10 - DUE_PERIOD_DAYS), LocalDate.now().minusDays(5), false);
        createSampleBorrowing(patronUser, testBook2, LocalDate.now().minusDays(20), LocalDate.now().minusDays(20 - DUE_PERIOD_DAYS), null, true); // Overdue

        // Fetch the history as the patron
        mockMvc.perform(get("/api/borrowings/history/user/{page}/{size}", 0, 5)
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Expecting the 2 records created for this user
                .andExpect(jsonPath("$[0].userId", is(patronUser.getId().toString())))
                .andExpect(jsonPath("$[1].userId", is(patronUser.getId().toString())));
    }

    // a librarian retrieving their own borrowing history (assuming librarians can have history).
    @Test
    void getUserBorrowingHistory_asLibrarian_getsOwnHistory_success() throws Exception {
        // Create a borrowing record for the librarian
        createSampleBorrowing(librarianUser, testBook1, LocalDate.now().minusDays(7), LocalDate.now().minusDays(7 - DUE_PERIOD_DAYS), null, false);

        // Fetch the history as the librarian
        mockMvc.perform(get("/api/borrowings/history/user/{page}/{size}", 0, 5)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Expecting the 1 record created for the librarian
                .andExpect(jsonPath("$[0].userId", is(librarianUser.getId().toString())));
    }

    // retrieving user borrowing history when the user has no records, expecting an empty list.
    @Test
    void getUserBorrowingHistory_noRecords_emptyList() throws Exception {
        // Fetch history for a user with no borrowing records
        mockMvc.perform(get("/api/borrowings/history/user/{page}/{size}", 0, 5)
                        .header("Authorization", "Bearer " + patronToken)) // Patron has no records initially
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Expecting an empty list
    }

    // retrieving all borrowing history records as a librarian.
    @Test
    void getAllBorrowingHistory_asLibrarian_success() throws Exception {
        // Create borrowing records for different users
        createSampleBorrowing(patronUser, testBook1, LocalDate.now().minusDays(10), LocalDate.now().minusDays(10-DUE_PERIOD_DAYS), LocalDate.now().minusDays(5), false);
        createSampleBorrowing(anotherPatron, testBook2, LocalDate.now().minusDays(8), LocalDate.now().minusDays(8-DUE_PERIOD_DAYS), null, false);
        // Note: Librarian borrowing record creation might not be possible via service/controller, but can exist in DB for testing history endpoint
        createSampleBorrowing(librarianUser, unavailableBook, LocalDate.now().minusDays(5), LocalDate.now().minusDays(5-DUE_PERIOD_DAYS), null, false); // Librarian can't borrow, but for test data

        // Fetch all history as librarian
        mockMvc.perform(get("/api/borrowings/history/all")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))); // Expecting all 3 records
    }

    // a patron user attempting to retrieve all borrowing history is forbidden.
    @Test
    void getAllBorrowingHistory_asPatron_forbidden() throws Exception {
        // Attempt to fetch all history as a patron
        mockMvc.perform(get("/api/borrowings/history/all")
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isForbidden());
    }

    // retrieving overdue books report as a librarian.
    @Test
    void getOverdueBooks_asLibrarian_success() throws Exception {
        // Create borrowing records, including one that is overdue and not returned
        createSampleBorrowing(patronUser, testBook1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(DUE_PERIOD_DAYS - 5), null, false); // Not overdue

        // Overdue and not returned
        createSampleBorrowing(anotherPatron, testBook2, LocalDate.now().minusDays(20), LocalDate.now().minusDays(20 - DUE_PERIOD_DAYS), null, true);
        // Overdue but returned
        createSampleBorrowing(patronUser, borrowedBook, LocalDate.now().minusDays(30), LocalDate.now().minusDays(30-DUE_PERIOD_DAYS), LocalDate.now().minusDays(1), true);


        // Fetch overdue books report as librarian
        mockMvc.perform(get("/api/borrowings/overdue/page/{page}/size/{size}", 0, 10)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Expecting only the one overdue and not-returned record
                .andExpect(jsonPath("$[0].userId", is(anotherPatron.getId().toString())))
                .andExpect(jsonPath("$[0].bookId", is(testBook2.getId().toString())))
                .andExpect(jsonPath("$[0].daysOverdue", is(greaterThan(0))));
    }

    // a patron user attempting to retrieve overdue books report is forbidden.
    @Test
    void getOverdueBooks_asPatron_forbidden() throws Exception {
        // Attempt to fetch overdue books report as a patron
        mockMvc.perform(get("/api/borrowings/overdue/page/{page}/size/{size}", 0, 10)
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isForbidden());
    }

    // retrieving overdue books report when there are no overdue books, expecting an empty list.
    @Test
    void getOverdueBooks_noOverdueBooks_emptyList() throws Exception {
        // Create borrowing records that are not overdue or are returned
        createSampleBorrowing(patronUser, testBook1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(DUE_PERIOD_DAYS-5), null, false); // Not overdue
        createSampleBorrowing(anotherPatron, testBook2, LocalDate.now().minusDays(2), LocalDate.now().plusDays(DUE_PERIOD_DAYS-2), LocalDate.now().minusDays(1), false); // Returned, not overdue

        // Fetch overdue books report as librarian
        mockMvc.perform(get("/api/borrowings/overdue/page/{page}/size/{size}", 0, 10)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Expecting an empty list
    }
}