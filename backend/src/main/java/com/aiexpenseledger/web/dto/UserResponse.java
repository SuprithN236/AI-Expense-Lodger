package com.aiexpenseledger.web.dto;

import com.aiexpenseledger.domain.User;

public record UserResponse(Long id, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
