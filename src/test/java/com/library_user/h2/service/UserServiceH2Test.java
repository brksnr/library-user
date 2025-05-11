package com.library_user.h2.service;

import com.library_user.exceptions.CustomException;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.helper.ErrorMessages;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.UserResponse;
import com.library_user.repository.UserRepository;
import com.library_user.security.JwtUtil;
import com.library_user.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionSystemException;


import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceH2Test {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    private User testPatron;
    private User testLibrarian;
    private final String defaultPasswordForUpdate = "newPassword123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        testPatron = User.builder()
                .name("Test Patron")
                .email("patron@example.com")
                .password(passwordEncoder.encode("password123"))
                .contact("1234567890")
                .role(Role.PATRON)
                .borrowedBookCount(0)
                .build();
        testPatron = userRepository.saveAndFlush(testPatron);

        testLibrarian = User.builder()
                .name("Test Librarian")
                .email("librarian@example.com")
                .password(passwordEncoder.encode("passwordlib"))
                .contact("0987654321")
                .role(Role.LIBRARIAN)
                .borrowedBookCount(0)
                .build();
        testLibrarian = userRepository.saveAndFlush(testLibrarian);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }


    // User registration with a new email
    @Test
    void register_whenEmailIsNotTaken_shouldCreateUserAndReturnUserResponse() {
        RegisterRequest registerRequest = new RegisterRequest(
                "New Patron",
                "newpatron@example.com",
                "newpassword",
                "1122334455",
                Role.PATRON
        );

        UserResponse userResponse = userService.register(registerRequest);

        assertNotNull(userResponse);
        assertNotNull(userResponse.id());
        assertEquals(registerRequest.name(), userResponse.name());
        assertEquals(registerRequest.email(), userResponse.email());
        assertEquals(registerRequest.role(), userResponse.role());
        assertEquals(0, userResponse.borrowedBookCount());

        User savedUser = userRepository.findByEmail(registerRequest.email()).orElseThrow(
                () -> new AssertionError("Kullanıcı veritabanına kaydedilmiş olmalıydı.")
        );
        assertTrue(passwordEncoder.matches(registerRequest.password(), savedUser.getPassword()));
        assertEquals(registerRequest.name(), savedUser.getName());
    }

    // Registration fails when email is already taken
    @Test
    void register_whenEmailIsAlreadyTaken_shouldThrowCustomException() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Another Patron",
                testPatron.getEmail(),
                "password",
                "5544332211",
                Role.PATRON
        );

        CustomException exception = assertThrows(CustomException.class, () -> userService.register(registerRequest));
        String expectedMessage = ErrorMessages.USER_NOT_FOUND_EMAIL + registerRequest.email();
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    }

    // Retrieving user by valid ID
    @Test
    void getUserById_whenUserExists_shouldReturnUserResponse() {
        UserResponse userResponse = userService.getUserById(testPatron.getId());

        assertNotNull(userResponse);
        assertEquals(testPatron.getId(), userResponse.id());
        assertEquals(testPatron.getName(), userResponse.name());
        assertEquals(testPatron.getEmail(), userResponse.email());
        assertEquals(testPatron.getRole(), userResponse.role());
    }

    // Retrieving user by invalid ID
    @Test
    void getUserById_whenUserDoesNotExist_shouldThrowCustomException() {
        UUID nonExistentId = UUID.randomUUID();
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserById(nonExistentId));

        String expectedMessage = ErrorMessages.USER_NOT_FOUND_ID + nonExistentId.toString();
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    // Getting all users when there are none
    @Test
    void getAllUsers_whenNoUsers_shouldReturnEmptyList() {
        userRepository.deleteAllInBatch();
        Pageable pageable = PageRequest.of(0, 10);
        List<UserResponse> users = userService.getAllUsers(pageable);
        assertTrue(users.isEmpty());
    }

    // Getting all users when users exist
    @Test
    void getAllUsers_whenUsersExist_shouldReturnUserResponseList() {
        Pageable pageable = PageRequest.of(0, 10);
        List<UserResponse> users = userService.getAllUsers(pageable);

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.email().equals(testPatron.getEmail())));
        assertTrue(users.stream().anyMatch(u -> u.email().equals(testLibrarian.getEmail())));
    }

    // Updating a valid patron user with valid data
    @Test
    void updateUser_whenUserExistsAndDataIsValidAndUserIsNotLibrarian_shouldUpdateAndReturnUserResponse() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "Updated Test Patron",
                "updated.patron@example.com",
                defaultPasswordForUpdate,
                "0000000000",
                Role.PATRON,
                1
        );

        UserResponse updatedUserResponse = userService.updateUser(testPatron.getId(), updateRequest);

        assertNotNull(updatedUserResponse);
        assertEquals(updateRequest.name(), updatedUserResponse.name());
        assertEquals(updateRequest.email(), updatedUserResponse.email());
        assertEquals(updateRequest.contact(), updatedUserResponse.contact());
        assertEquals(updateRequest.role(), updatedUserResponse.role());
        assertEquals(updateRequest.borrowedBookCount(), updatedUserResponse.borrowedBookCount());

        User fetchedUser = userRepository.findById(testPatron.getId()).orElseThrow();
        assertEquals(updateRequest.name(), fetchedUser.getName());
        assertEquals(updateRequest.email(), fetchedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", fetchedUser.getPassword()), "Şifre değişmemeliydi (servis şifre güncellemiyor).");
    }

    // Updating a non-existent user
    @Test
    void updateUser_whenUserDoesNotExist_shouldThrowCustomException() {
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "N/A", "na@example.com", defaultPasswordForUpdate, "N/A", Role.PATRON, 0
        );
        CustomException exception = assertThrows(CustomException.class, () -> userService.updateUser(nonExistentId, updateRequest));
        String expectedMessage = ErrorMessages.USER_NOT_FOUND_ID + nonExistentId.toString();
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    // Updating user email to another user's email
    @Test
    void updateUser_whenEmailIsTakenByAnotherUser_shouldThrowCustomException() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                testPatron.getName(),
                testLibrarian.getEmail(),
                defaultPasswordForUpdate,
                testPatron.getContact(),
                testPatron.getRole(),
                testPatron.getBorrowedBookCount()
        );

        CustomException exception = assertThrows(CustomException.class, () -> userService.updateUser(testPatron.getId(), updateRequest));
        assertEquals(ErrorMessages.EMAIL_ALREADY_IN_USE, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    }

    // Updating a librarian should fail
    @Test
    void updateUser_whenAttemptingToUpdateALibrarian_shouldThrowCustomException() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "Updated Librarian Name",
                testLibrarian.getEmail(),
                defaultPasswordForUpdate,
                "1111111111",
                Role.LIBRARIAN,
                0
        );
        CustomException exception = assertThrows(CustomException.class, () -> userService.updateUser(testLibrarian.getId(), updateRequest));
        assertEquals(ErrorMessages.LIBRARIAN_UPDATE, exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    }

    // Changing patron role to librarian should succeed
    @Test
    void updateUser_whenChangingPatronRoleToLibrarian_shouldSucceed() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                testPatron.getName(),
                "patron.becoming.lib@example.com",
                defaultPasswordForUpdate,
                testPatron.getContact(),
                Role.LIBRARIAN,
                testPatron.getBorrowedBookCount()
        );

        UserResponse updatedUserResponse = userService.updateUser(testPatron.getId(), updateRequest);
        assertNotNull(updatedUserResponse);
        assertEquals(Role.LIBRARIAN, updatedUserResponse.role());
        assertEquals("patron.becoming.lib@example.com", updatedUserResponse.email());

        User fetchedUser = userRepository.findById(testPatron.getId()).orElseThrow();
        assertEquals(Role.LIBRARIAN, fetchedUser.getRole());
    }

    // Updating user with same email should succeed
    @Test
    void updateUser_whenUpdatingOwnEmailToSameEmail_shouldSucceed() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                testPatron.getName(),
                testPatron.getEmail(),
                defaultPasswordForUpdate,
                "newContact123",
                testPatron.getRole(),
                testPatron.getBorrowedBookCount()
        );

        UserResponse updatedUserResponse = userService.updateUser(testPatron.getId(), updateRequest);

        assertNotNull(updatedUserResponse);
        assertEquals(testPatron.getEmail(), updatedUserResponse.email());
        assertEquals("newContact123", updatedUserResponse.contact());
    }

    // Validation fails when borrowed book count exceeds max allowed
    @Test
    void updateUser_whenBorrowedBookCountExceedsMax_shouldThrowConstraintViolationException() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                testPatron.getName(),
                "patron.books.over@example.com",
                defaultPasswordForUpdate,
                testPatron.getContact(),
                testPatron.getRole(),
                10
        );

        TransactionSystemException exception = assertThrows(TransactionSystemException.class, () -> {
            userService.updateUser(testPatron.getId(), updateRequest);
        });

        Throwable cause = exception.getCause();
        while (cause != null && !(cause instanceof ConstraintViolationException)) {
            cause = cause.getCause();
        }
        assertNotNull(cause);
        assertTrue(true, "Sebep ConstraintViolationException tipinde olmalı. Alınan: " + cause.getClass().getName());

        ConstraintViolationException cve = (ConstraintViolationException) cause;
        Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();

        assertTrue(violations.stream()
                .anyMatch(cv -> cv.getPropertyPath().toString().equals("borrowedBookCount") &&
                        cv.getMessage().toLowerCase().contains("less than or equal to 5")
                ));
    }

    // Updating a user with a negative book count fails with a constraint error.
    @Test
    void updateUser_whenBorrowedBookCountIsNegative_shouldThrowConstraintViolationException() {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                testPatron.getName(),
                "patron.books.negative@example.com",
                defaultPasswordForUpdate,
                testPatron.getContact(),
                testPatron.getRole(),
                -1
        );

        TransactionSystemException exception = assertThrows(TransactionSystemException.class, () -> {
            userService.updateUser(testPatron.getId(), updateRequest);
        });

        Throwable cause = exception.getCause();
        while (cause != null && !(cause instanceof ConstraintViolationException)) {
            cause = cause.getCause();
        }
        assertNotNull(cause);
        assertTrue(true, cause.getClass().getName());

        ConstraintViolationException cve = (ConstraintViolationException) cause;
        Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();

        assertTrue(violations.stream()
                .anyMatch(cv -> cv.getPropertyPath().toString().equals("borrowedBookCount") &&
                        cv.getMessage().toLowerCase().contains("greater than or equal to 0")
                ));
    }

    // Successfully deleting an existing user.
    @Test
    void deleteUser_whenUserExists_shouldDeleteUser() {
        UUID userIdToDelete = testPatron.getId();
        assertTrue(userRepository.existsById(userIdToDelete));

        userService.deleteUser(userIdToDelete);

        assertFalse(userRepository.existsById(userIdToDelete));
    }

    // Deleting a non-existent user throws a custom "not found" error.
    @Test
    void deleteUser_whenUserDoesNotExist_shouldThrowCustomException() {
        UUID nonExistentId = UUID.randomUUID();
        CustomException exception = assertThrows(CustomException.class, () -> userService.deleteUser(nonExistentId));
        String expectedMessage = ErrorMessages.USER_NOT_FOUND_ID + nonExistentId.toString();
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }
}
