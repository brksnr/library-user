package com.library_user.model.dto;

import com.library_user.model.entity.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    private String contact;
    private Role role;
    private Integer borrowedBookCount;
}
