package com.library_user.service.Impl;

import com.library_user.exceptions.CustomException;
import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import com.library_user.model.helper.ErrorMessages;
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

    @Override
    public UserResponse getUserById(UUID id) {
        User user = isUserFound(id);
        return UserResponse.fromUser(user);
    }

    @Override
    public List<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromUser)
                .getContent();
    }

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

    @Override
    public void deleteUser(UUID id) {
        isUserFound(id);
        userRepository.deleteById(id);
    }

    public void isLibrarianThrowException(UUID id, Role role){
        if(userRepository.existsByIdAndRole(id, Role.LIBRARIAN)){
            throw new CustomException(String.format(ErrorMessages.LIBRARIAN_UPDATE), HttpStatus.FORBIDDEN);
        }
    }

    public User isUserFound(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(String.format(ErrorMessages.USER_NOT_FOUND_ID) + id, HttpStatus.NOT_FOUND));
    }

    public void isMailExist(UUID id, String email) {
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new CustomException(String.format(ErrorMessages.EMAIL_ALREADY_IN_USE), HttpStatus.CONFLICT);
        }
    }
}