package com.aiexpenseledger.web.dto;

import com.aiexpenseledger.domain.Group;

import java.util.Comparator;
import java.util.List;

public record GroupResponse(Long id, String name, List<UserResponse> members) {

    public static GroupResponse from(Group group) {
        List<UserResponse> members = group.getMembers().stream()
                .map(UserResponse::from)
                .sorted(Comparator.comparing(UserResponse::email))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), members);
    }
}
