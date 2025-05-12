package com.library_user.h2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.repository.BookRepository;
import com.library_user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookControllerH2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String librarianToken;
    private String patronToken;

    private final String DEFAULT_TITLE = "Effective Java";
    private final String DEFAULT_AUTHOR = "Joshua Bloch";
    private final String DEFAULT_ISBN = "978-0134685991";
    private final String DEFAULT_DESCRIPTION = "A comprehensive guide to the Java platform.";
    private final LocalDate DEFAULT_PUB_DATE = LocalDate.of(2018, 1, 6);
    private final String DEFAULT_GENRE = "Programming";
    private final boolean DEFAULT_AVAILABILITY = true;


    @BeforeEach
    void setUp() throws Exception {

        objectMapper.findAndRegisterModules();


        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Test kullanıcıları oluştur
        User librarianUser = User.builder()
                .name("Librarian User")
                .email("librarian@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("1234567890")
                .role(Role.LIBRARIAN)
                .borrowedBookCount(0)
                .build();
        userRepository.save(librarianUser);

        User patronUser = User.builder()
                .name("Patron User")
                .email("patron@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("0987654321")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();
        userRepository.save(patronUser);

        librarianToken = authenticateAndGetToken("librarian@example.com", "password123");
        patronToken = authenticateAndGetToken("patron@example.com", "password123");
    }

    @AfterEach
    void tearDown() {
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

    private BookDto createDefaultBookDto(UUID id) {
        return new BookDto(
                id,
                DEFAULT_TITLE,
                DEFAULT_AUTHOR,
                DEFAULT_ISBN,
                DEFAULT_DESCRIPTION,
                DEFAULT_PUB_DATE,
                DEFAULT_GENRE,
                DEFAULT_AVAILABILITY
        );
    }

    private Book createAndSaveDefaultBook() {
        Book book = Book.builder()
                .title(DEFAULT_TITLE)
                .author(DEFAULT_AUTHOR)
                .isbn(DEFAULT_ISBN)
                .description(DEFAULT_DESCRIPTION)
                .publicationDate(DEFAULT_PUB_DATE)
                .genre(DEFAULT_GENRE)
                .availability(DEFAULT_AVAILABILITY)
                .build();
        return bookRepository.save(book);
    }

    // successfully adding a book as a librarian.
    @Test
    void addBook_asLibrarian_success() throws Exception {
        BookDto newBookDto = createDefaultBookDto(null);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBookDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is(DEFAULT_TITLE)))
                .andExpect(jsonPath("$.isbn", is(DEFAULT_ISBN)))
                .andExpect(jsonPath("$.description", is(DEFAULT_DESCRIPTION)));
    }

    // adding a book as a patron is forbidden.
    @Test
    void addBook_asPatron_forbidden() throws Exception {
        BookDto newBookDto = new BookDto(null, "Forbidden Book", "Author", "1112223330", "Desc", LocalDate.now(), "Test", true);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBookDto)))
                .andExpect(status().isForbidden());
    }

    // adding a book without a token is forbidden.
    @Test
    void addBook_missingToken_forbidden() throws Exception {
        BookDto newBookDto = new BookDto(null, "Unauthorized Book", "Author", "1112223340", "Desc", LocalDate.now(), "Test", true);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBookDto)))
                .andExpect(status().isForbidden()); // Expecting Forbidden if no token is provided
    }

    // adding a book with an existing ISBN returns a conflict error.
    @Test
    void addBook_withExistingIsbn_conflict() throws Exception {
        createAndSaveDefaultBook(); // Save a book with the default ISBN

        BookDto secondBookDto = new BookDto(null, "Another Book with Same ISBN", "Another Author", DEFAULT_ISBN, "Another Desc", LocalDate.now(), "Fiction", true);
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBookDto)))
                .andExpect(status().isConflict()); // Expecting Conflict for duplicate ISBN
    }

    // adding a book with invalid input (e.g., blank title) returns a bad request error.
    @Test
    void addBook_invalidInput_badRequest() throws Exception {
        BookDto invalidBookDto = new BookDto(null, "", DEFAULT_AUTHOR, "1234567000", DEFAULT_DESCRIPTION, DEFAULT_PUB_DATE, DEFAULT_GENRE, true);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBookDto)))
                .andExpect(status().isBadRequest()); // Expecting Bad Request for invalid input
    }



    // retrieving a book by ID as a librarian.
    @Test
    void getBookById_asLibrarian_success() throws Exception {
        Book savedBook = createAndSaveDefaultBook();

        mockMvc.perform(get("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBook.getId().toString())))
                .andExpect(jsonPath("$.title", is(DEFAULT_TITLE)));
    }

    // retrieving a book by ID as a patron.
    @Test
    void getBookById_asPatron_success() throws Exception {
        Book savedBook = createAndSaveDefaultBook();

        mockMvc.perform(get("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBook.getId().toString())))
                .andExpect(jsonPath("$.title", is(DEFAULT_TITLE)));
    }

    // attempting to retrieve a non-existent book by ID returns a not found error.
    @Test
    void getBookById_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID(); // Non-existent ID
        mockMvc.perform(get("/api/books/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + librarianToken)) // Use librarian token for permission
                .andExpect(status().isNotFound()); // Expecting Not Found
    }

    // successfully updating a book as a librarian.
    @Test
    void updateBook_asLibrarian_success() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Old Title").author("Old Author").isbn("1111111111").description("Old Desc").publicationDate(LocalDate.now().minusDays(1)).genre("Old Genre").availability(true).build()); // Başlangıçta da geçerli bir tarih olsun
        LocalDate validPublicationDate = LocalDate.now();

        BookDto updatedInfo = new BookDto(savedBook.getId(),
                "New Title",
                "New Author",
                "2222222222",
                "New Desc",
                validPublicationDate,
                "New Genre",
                false);

        mockMvc.perform(put("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Title")))
                .andExpect(jsonPath("$.author", is("New Author")))
                .andExpect(jsonPath("$.isbn", is("2222222222")))
                .andExpect(jsonPath("$.description", is("New Desc")))
                .andExpect(jsonPath("$.publicationDate", is(validPublicationDate.toString())));
    }


    // try to update a book with empty fields
    @Test
    void updateBook_whenTitleIsBlank_shouldReturnBadRequest() throws Exception {
        Book savedBook = bookRepository.save(Book.builder()
                .title("valid title")
                .author("valid author")
                .isbn("1234567890")
                .description("valid description")
                .publicationDate(LocalDate.now().minusDays(10))
                .genre("valid genre")
                .availability(true)
                .build());

        BookDto invalidUpdateInfo = new BookDto(
                savedBook.getId(),
                "",
                "update author",
                "0987654321",
                "update description",
                LocalDate.now(),
                "update gnre",
                false);

        mockMvc.perform(put("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateInfo)))
                .andExpect(status().isBadRequest());
    }

    // try to update when publication date is future
    @Test
    void updateBook_whenPublicationDateIsFuture_shouldReturnBadRequest() throws Exception {

        Book savedBook = bookRepository.save(Book.builder()
                .title("test")
                .author("test")
                .isbn("1122334455")
                .description("test")
                .publicationDate(LocalDate.now().minusDays(5))
                .genre("test")
                .availability(true)
                .build());


        BookDto invalidUpdateInfo = new BookDto(
                savedBook.getId(),
                "updated title",
                "updated author",
                "5544332211",
                "updated description",
                LocalDate.now().plusDays(1),
                "updated genre",
                false);

        mockMvc.perform(put("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateInfo)))
                .andExpect(status().isBadRequest());
    }

    // a patron attempting to update a book is forbidden.
    @Test
    void updateBook_asPatron_forbidden() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Patron Cannot Update").author("Author").isbn("3333333333").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());
        BookDto updatedInfo = new BookDto(savedBook.getId(), "Attempted Update", "Author", "3333333333", "Desc", LocalDate.now(), "Test", true);

        mockMvc.perform(put("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isForbidden()); // Expecting Forbidden
    }

    // attempting to update a non-existent book returns a not found error.
    @Test
    void updateBook_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID(); // Non-existent ID
        BookDto updatedInfo = new BookDto(nonExistentId, "Non Existent Update", "Author", "4444444444", "Desc", LocalDate.now(), "Test", true);

        mockMvc.perform(put("/api/books/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + librarianToken) // Use librarian token for permission
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isNotFound()); // Expecting Not Found
    }

    // successfully deleting a book as a librarian.
    @Test
    void deleteBook_asLibrarian_success() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("To Be Deleted").author("Author").isbn("5555555555").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());

        mockMvc.perform(delete("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNoContent()); // Expecting 204 No Content

        // Verify the book is no longer retrievable
        mockMvc.perform(get("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNotFound());
    }

    // a patron attempting to delete a book is forbidden.
    @Test
    void deleteBook_asPatron_forbidden() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Patron Cannot Delete").author("Author").isbn("6666666666").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());

        mockMvc.perform(delete("/api/books/{id}", savedBook.getId())
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isForbidden()); // Expecting Forbidden
    }

    // attempting to delete a non-existent book returns a not found error.
    @Test
    void deleteBook_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID(); // Non-existent ID
        mockMvc.perform(delete("/api/books/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + librarianToken)) // Use librarian token for permission
                .andExpect(status().isNotFound()); // Expecting Not Found
    }


    // retrieving a book by ISBN as a librarian.
    @Test
    void getBookByIsbn_asLibrarian_success() throws Exception {
        String uniqueIsbn = "999888777";
        bookRepository.save(Book.builder().title("Book by ISBN").author("Author").isbn(uniqueIsbn).description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());

        mockMvc.perform(get("/api/books/isbn/{isbn}", uniqueIsbn)
                        .header("Authorization", "Bearer " + librarianToken)) // Test as librarian
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn", is(uniqueIsbn)));
    }

    // attempting to retrieve a book by a non-existent ISBN returns a not found error.
    @Test
    void getBookByIsbn_notFound() throws Exception {
        String nonExistentIsbn = "000000000"; // Non-existent ISBN
        mockMvc.perform(get("/api/books/isbn/{isbn}", nonExistentIsbn)
                        .header("Authorization", "Bearer " + librarianToken)) // Use librarian token for permission
                .andExpect(status().isNotFound()); // Expecting Not Found
    }

    // successfully setting a book's availability to false as a librarian.
    @Test
    void updateBookAvailability_asLibrarian_setUnavailable_success() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Availability Test").author("Author").isbn("7777777777").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());

        mockMvc.perform(patch("/api/books/{id}/availability/{availability}", savedBook.getId(), false)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBook.getId().toString())))
                .andExpect(jsonPath("$.availability", is(false)));
    }

    // successfully setting a book's availability to true as a librarian.
    @Test
    void updateBookAvailability_asLibrarian_setAvailable_success() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Availability Test").author("Author").isbn("7777777778").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(false).build());

        mockMvc.perform(patch("/api/books/{id}/availability/{availability}", savedBook.getId(), true)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBook.getId().toString())))
                .andExpect(jsonPath("$.availability", is(true)));
    }

    // a patron attempting to update book availability is forbidden.
    @Test
    void updateBookAvailability_asPatron_forbidden() throws Exception {
        Book savedBook = bookRepository.save(Book.builder().title("Patron Cannot Update Availability").author("Author").isbn("8888888888").description("Desc").publicationDate(LocalDate.now()).genre("Test").availability(true).build());

        mockMvc.perform(patch("/api/books/{id}/availability/{availability}", savedBook.getId(), false)
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isForbidden()); // Expecting Forbidden
    }

    // attempting to update availability for a non-existent book returns a not found error.
    @Test
    void updateBookAvailability_bookNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID(); // Non-existent ID
        mockMvc.perform(patch("/api/books/{id}/availability/{availability}", nonExistentId, false)
                        .header("Authorization", "Bearer " + librarianToken)) // Use librarian token for permission
                .andExpect(status().isNotFound()); // Expecting Not Found
    }

    // searching for books by title returns matching books.
    @Test
    void searchBooksByTitle_success() throws Exception {
        bookRepository.save(Book.builder().title("UniqueTitleSearch").author("AuthorA").isbn("1000000001").description("DescA").publicationDate(LocalDate.now()).genre("GenreA").availability(true).build());
        bookRepository.save(Book.builder().title("Another UniqueTitleSearch").author("AuthorB").isbn("1000000002").description("DescB").publicationDate(LocalDate.now()).genre("GenreB").availability(false).build());
        bookRepository.save(Book.builder().title("Different").author("AuthorC").isbn("1000000003").description("DescC").publicationDate(LocalDate.now()).genre("GenreC").availability(true).build());

        mockMvc.perform(get("/api/books/search/title/{title}", "UniqueTitleSearch")
                        .header("Authorization", "Bearer " + patronToken)) // Patrons can search
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1)))) // Expecting at least 1 match
                .andExpect(jsonPath("$[0].title", containsString("UniqueTitleSearch")));
    }

    // searching for books by author returns matching books.
    @Test
    void searchBooksByAuthor_success() throws Exception {
        String searchAuthor = "SearchableAuthor";
        bookRepository.save(Book.builder().title("Book By Author1").author(searchAuthor).isbn("2000000001").description("DescX").publicationDate(LocalDate.now()).genre("GenreX").availability(true).build());
        bookRepository.save(Book.builder().title("Book By Author2").author(searchAuthor).isbn("2000000002").description("DescY").publicationDate(LocalDate.now()).genre("GenreY").availability(true).build());

        mockMvc.perform(get("/api/books/search/author/{author}", searchAuthor)
                        .header("Authorization", "Bearer " + librarianToken)) // Librarians can search
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Expecting 2 matches
                .andExpect(jsonPath("$[0].author", is(searchAuthor)))
                .andExpect(jsonPath("$[1].author", is(searchAuthor)));
    }

    // searching for books by genre returns matching books.
    @Test
    void searchBooksByGenre_success() throws Exception {
        String searchGenre = "SearchableGenre";
        bookRepository.save(Book.builder().title("Book Genre 1").author("Author G1").isbn("3000000001").description("DescG1").publicationDate(LocalDate.now()).genre(searchGenre).availability(true).build());
        bookRepository.save(Book.builder().title("Book Genre 2").author("Author G2").isbn("3000000002").description("DescG2").publicationDate(LocalDate.now()).genre(searchGenre).availability(false).build());

        mockMvc.perform(get("/api/books/search/genre/{genre}", searchGenre)
                        .header("Authorization", "Bearer " + patronToken)) // Patrons can search
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Expecting 2 matches
                .andExpect(jsonPath("$[0].genre", is(searchGenre)));
    }

    // searching for available books returns only available books.
    @Test
    void searchBooksByAvailability_available_success() throws Exception {
        bookRepository.save(Book.builder().title("Available Book 1").author("Author Avail").isbn("4000000001").description("DescAvail1").publicationDate(LocalDate.now()).genre("AvailGenre").availability(true).build());
        bookRepository.save(Book.builder().title("Unavailable Book 1").author("Author Avail").isbn("4000000002").description("DescAvail2").publicationDate(LocalDate.now()).genre("AvailGenre").availability(false).build());
        bookRepository.save(Book.builder().title("Available Book 2").author("Author Avail").isbn("4000000003").description("DescAvail3").publicationDate(LocalDate.now()).genre("AvailGenre").availability(true).build());


        mockMvc.perform(get("/api/books/search/availability/{availability}", true)
                        .header("Authorization", "Bearer " + patronToken)) // Patrons can search
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Expecting the 2 available books
                .andExpect(jsonPath("$[0].availability", is(true)))
                .andExpect(jsonPath("$[1].availability", is(true)));
    }

    // searching for unavailable books returns only unavailable books.
    @Test
    void searchBooksByAvailability_unavailable_success() throws Exception {
        bookRepository.save(Book.builder().title("Available Book X").author("Author Unavail").isbn("5000000001").description("DescUnavail1").publicationDate(LocalDate.now()).genre("UnavailGenre").availability(true).build());
        bookRepository.save(Book.builder().title("Unavailable Book X").author("Author Unavail").isbn("5000000002").description("DescUnavail2").publicationDate(LocalDate.now()).genre("UnavailGenre").availability(false).build());

        mockMvc.perform(get("/api/books/search/availability/{availability}", false)
                        .header("Authorization", "Bearer " + patronToken)) // Patrons can search
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Expecting the 1 unavailable book
                .andExpect(jsonPath("$[0].availability", is(false)));
    }
}