package com.aiexpenseledger.web;

import com.aiexpenseledger.security.AuthenticatedUser;
import com.aiexpenseledger.service.GroupService;
import com.aiexpenseledger.web.dto.AddMemberRequest;
import com.aiexpenseledger.web.dto.CreateGroupRequest;
import com.aiexpenseledger.web.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupResponse> listMyGroups(@AuthenticationPrincipal AuthenticatedUser user) {
        return groupService.listGroupsForUser(user.id()).stream().map(GroupResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CreateGroupRequest request) {
        return GroupResponse.from(groupService.createGroup(request.name(), user.id()));
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long groupId) {
        return GroupResponse.from(groupService.getGroupForMember(groupId, user.id()));
    }

    @PostMapping("/{groupId}/members")
    public GroupResponse addMember(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long groupId,
                                   @Valid @RequestBody AddMemberRequest request) {
        return GroupResponse.from(groupService.addMember(groupId, request.email(), user.id()));
    }
}
