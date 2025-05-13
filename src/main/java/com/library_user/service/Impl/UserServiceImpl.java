package com.library_user.service.Impl;

import com.library_user.exceptions.CustomException;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.helper.ErrorMessages;
import com.library_user.model.request.AuthRequest;
import com.library_user.model.request.RegisterRequest;
import com.library_user.model.request.UpdateUserRequest;
import com.library_user.model.response.AuthResponse;
import com.library_user.model.response.UserResponse;
import com.library_user.repository.UserRepository;
import com.library_user.security.JwtUtil;
import com.library_user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     Authenticates the user with email and password, returns a JWT token if successful
     * */
    @Override
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token);
    }

    /**
     Registers a new user, throws an error if the email is already in use
     TODO:Both PATRON AND LIBRARIAN can be registered. We can designate a single librarian and the librarian can be given the authority to make everyone else a librarian.
     **/
    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(String.format(ErrorMessages.USER_NOT_FOUND_EMAIL) + request.email(), HttpStatus.CONFLICT);
        }
        int borrowedBookCount = 0;
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .contact(request.contact())
                .role(request.role())
                .borrowedBookCount(borrowedBookCount)
                .build();
        User saved = userRepository.save(user);
        return UserResponse.fromUser(saved);
    }

    /**
     Retrieves a user by their ID, throws an error if not found
     * */
    @Override
    public UserResponse getUserById(UUID id) {
        User user = isUserFound(id);
        return UserResponse.fromUser(user);
    }


    /**
     Returns all users with pagination support
     **/
    @Override
    public List<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromUser)
                .getContent();
    }

    /**
     Updates user information, throws an error if email is already taken or user is a librarian
     */
    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = isUserFound(id);
        isMailExist(id, request.email());
        isLibrarianThrowException(id, Role.LIBRARIAN);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setContact(request.contact());
        user.setRole(request.role());
        user.setBorrowedBookCount(request.borrowedBookCount());
        User updated = userRepository.save(user);
        return UserResponse.fromUser(updated);
    }

    /**
     Deletes a user by ID, throws an error if user is not found
     */
    @Override
    public void deleteUser(UUID id) {
        isUserFound(id);
        userRepository.deleteById(id);
    }

    /**
     Throws an error if the user is a librarian, not allowed to update
     */
    public void isLibrarianThrowException(UUID id, Role role){
        if(userRepository.existsByIdAndRole(id, Role.LIBRARIAN)){
            throw new CustomException(String.format(ErrorMessages.LIBRARIAN_UPDATE), HttpStatus.FORBIDDEN);
        }
    }

    /**
     Checks if the user exists by ID, throws an error if not found
     */
    public User isUserFound(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(String.format(ErrorMessages.USER_NOT_FOUND_ID) + id, HttpStatus.NOT_FOUND));
    }

    /**
     Checks if the email is already used by another user , throws an error if so
     */
    public void isMailExist(UUID id, String email) {
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new CustomException(String.format(ErrorMessages.EMAIL_ALREADY_IN_USE), HttpStatus.CONFLICT);
        }
    }
}