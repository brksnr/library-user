package com.library_user.h2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import com.library_user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerH2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String librarianToken;
    private User patronUser;
    private User librarianUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        RegisterRequest librarianRegRequest = RegisterRequest.builder()
                .name("Admin User")
                .email("admin@example.com")
                .password("password123")
                .contact("1234567890")
                .role(Role.LIBRARIAN)
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(librarianRegRequest)))
                .andExpect(status().isCreated());

        librarianUser = userRepository.findByEmail("admin@example.com").orElseThrow();


        AuthRequest authRequestLibrarian = AuthRequest.builder().email("admin@example.com").password("password123").build();
        MvcResult resultLibrarian = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequestLibrarian)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse authResponseLibrarian = objectMapper.readValue(resultLibrarian.getResponse().getContentAsString(), AuthResponse.class);
        librarianToken = authResponseLibrarian.token();

        RegisterRequest patronRegRequest = RegisterRequest.builder()
                .name("Patron User")
                .email("patron@example.com")
                .password("password123")
                .contact("0987654321")
                .role(Role.PATRON)
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patronRegRequest)))
                .andExpect(status().isCreated());
        patronUser = userRepository.findByEmail("patron@example.com").orElseThrow();
    }

    // successful user registration.
    @Test
    void registerUser_success() throws Exception {
        RegisterRequest newUserRequest = RegisterRequest.builder()
                .name("New Patron")
                .email("newpatron@example.com")
                .password("securePassword")
                .contact("1122334455")
                .role(Role.PATRON)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Patron")))
                .andExpect(jsonPath("$.email", is("newpatron@example.com")))
                .andExpect(jsonPath("$.role", is(Role.PATRON.toString())))
                .andExpect(jsonPath("$.borrowedBookCount", is(0)));

        assertTrue(userRepository.findByEmail("newpatron@example.com").isPresent());
    }

    // user registration with an email that is already in use.
    @Test
    void registerUser_emailAlreadyInUse() throws Exception {
        RegisterRequest duplicateEmailRequest = RegisterRequest.builder()
                .name("Another User")
                .email("patron@example.com") // Existing email
                .password("securePassword")
                .contact("5544332211")
                .role(Role.PATRON)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isConflict());
    }

    // user registration with invalid input (blank name).
    @Test
    void registerUser_invalidInput_blankName() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .name("") // Invalid input
                .email("invalid@example.com")
                .password("password123")
                .contact("123123123")
                .role(Role.PATRON)
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }


    // successful user login.
    @Test
    void login_success() throws Exception {
        AuthRequest authRequest = AuthRequest.builder()
                .email("patron@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // user login with incorrect password.
    @Test
    void login_invalidCredentials_wrongPassword() throws Exception {
        AuthRequest authRequest = AuthRequest.builder()
                .email("patron@example.com")
                .password("wrongpassword") // Incorrect password
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    // user login when the user is not found.
    @Test
    void login_invalidCredentials_userNotFound() throws Exception {
        AuthRequest authRequest = AuthRequest.builder()
                .email("nonexistent@example.com") // Non-existent user
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }


    // retrieving a user by ID as a librarian user.
    @Test
    void getUserById_asLibrarian_success() throws Exception {
        mockMvc.perform(get("/api/auth/{id}", patronUser.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patronUser.getId().toString())))
                .andExpect(jsonPath("$.email", is(patronUser.getEmail())));
    }

    // attempting to retrieve a non-existent user by ID as a librarian.
    @Test
    void getUserById_asLibrarian_notFound() throws Exception {
        UUID randomId = UUID.randomUUID(); // Non-existent ID
        mockMvc.perform(get("/api/auth/{id}", randomId)
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // a patron user attempting to retrieve another user's information by ID.
    @Test
    void getUserById_asPatron_forbidden() throws Exception {
        AuthRequest patronAuthRequest = AuthRequest.builder().email(patronUser.getEmail()).password("password123").build();
        MvcResult patronLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patronAuthRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse patronAuthResponse = objectMapper.readValue(patronLoginResult.getResponse().getContentAsString(), AuthResponse.class);
        String patronToken = patronAuthResponse.token();

        mockMvc.perform(get("/api/auth/{id}", librarianUser.getId()) // Accessing another user's ID
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }


    // retrieving all users as a librarian.
    @Test
    void getAllUsers_asLibrarian_success() throws Exception {
        mockMvc.perform(get("/api/auth")
                        .header("Authorization", "Bearer " + librarianToken)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Expecting the initial two users
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[1].email").isNotEmpty());
    }

    // a patron user attempting to retrieve all users.
    @Test
    void getAllUsers_asPatron_forbidden() throws Exception {
        AuthRequest patronAuthRequest = AuthRequest.builder().email(patronUser.getEmail()).password("password123").build();
        MvcResult patronLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patronAuthRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse patronAuthResponse = objectMapper.readValue(patronLoginResult.getResponse().getContentAsString(), AuthResponse.class);
        String patronToken = patronAuthResponse.token();

        mockMvc.perform(get("/api/auth") // Accessing all users endpoint
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // successfully updating a user as a librarian.
    @Test
    void updateUser_asLibrarian_success() throws Exception {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name("Patron User Updated")
                .contact("111000111")
                .build();

        mockMvc.perform(put("/api/auth/{id}", patronUser.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Patron User Updated")))
                .andExpect(jsonPath("$.contact", is("111000111")))
                .andExpect(jsonPath("$.email", is(patronUser.getEmail())));

        User updatedUser = userRepository.findById(patronUser.getId()).orElseThrow();
        assertEquals("Patron User Updated", updatedUser.getName());
        assertEquals("111000111", updatedUser.getContact());
    }

    // attempting to update a user's email to an existing email as a librarian.
    @Test
    void updateUser_asLibrarian_updateEmailToExisting() throws Exception {
        RegisterRequest tempUserRequest = RegisterRequest.builder()
                .name("Temporary User")
                .email("temp@example.com") // Existing email created
                .password("password123")
                .contact("555555555")
                .role(Role.PATRON)
                .build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tempUserRequest)))
                .andExpect(status().isCreated());

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .email(librarianUser.getEmail()) // Attempting to use an existing email
                .build();

        mockMvc.perform(put("/api/auth/{id}", patronUser.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }

    // attempting to update a non-existent user as a librarian.
    @Test
    void updateUser_asLibrarian_userNotFound() throws Exception {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().name("Doesn't Matter").build();
        UUID randomId = UUID.randomUUID(); // Non-existent user ID

        mockMvc.perform(put("/api/auth/{id}", randomId)
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    // a patron user attempting to update another user's information.
    @Test
    void updateUser_asPatron_forbidden() throws Exception {
        AuthRequest patronAuthRequest = AuthRequest.builder().email(patronUser.getEmail()).password("password123").build();
        MvcResult patronLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patronAuthRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse patronAuthResponse = objectMapper.readValue(patronLoginResult.getResponse().getContentAsString(), AuthResponse.class);
        String patronToken = patronAuthResponse.token();

        UpdateUserRequest updateRequest = UpdateUserRequest.builder().name("Attempted Update").build();

        mockMvc.perform(put("/api/auth/{id}", librarianUser.getId()) // Attempting to update another user
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }


    // successfully deleting a user as a librarian.
    @Test
    void deleteUser_asLibrarian_success() throws Exception {
        RegisterRequest toDeleteRequest = RegisterRequest.builder()
                .name("ToDelete")
                .email("todelete@example.com")
                .password("password123")
                .contact("000000000")
                .role(Role.PATRON)
                .build();
        MvcResult createdResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toDeleteRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        UserResponse userToDelete = objectMapper.readValue(createdResult.getResponse().getContentAsString(), UserResponse.class);
        UUID userIdToDelete = userToDelete.id();

        assertTrue(userRepository.findById(userIdToDelete).isPresent());

        mockMvc.perform(delete("/api/auth/{id}", userIdToDelete)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNoContent()); // Expecting 204 No Content

        assertFalse(userRepository.findById(userIdToDelete).isPresent());
    }

    // attempting to delete a non-existent user as a librarian.
    @Test
    void deleteUser_asLibrarian_userNotFound() throws Exception {
        UUID randomId = UUID.randomUUID(); // Non-existent user ID
        mockMvc.perform(delete("/api/auth/{id}", randomId)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNotFound());
    }

    // a patron user attempting to delete another user.
    @Test
    void deleteUser_asPatron_forbidden() throws Exception {
        AuthRequest patronAuthRequest = AuthRequest.builder().email(patronUser.getEmail()).password("password123").build();
        MvcResult patronLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patronAuthRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse patronAuthResponse = objectMapper.readValue(patronLoginResult.getResponse().getContentAsString(), AuthResponse.class);
        String patronToken = patronAuthResponse.token();

        mockMvc.perform(delete("/api/auth/{id}", librarianUser.getId()) // Attempting to delete another user
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isForbidden());
    }
}