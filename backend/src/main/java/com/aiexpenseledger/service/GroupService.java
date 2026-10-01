package com.aiexpenseledger.service;

import com.aiexpenseledger.domain.Group;
import com.aiexpenseledger.domain.User;
import com.aiexpenseledger.exception.GroupAccessDeniedException;
import com.aiexpenseledger.exception.LedgerValidationException;
import com.aiexpenseledger.exception.ResourceNotFoundException;
import com.aiexpenseledger.repository.GroupRepository;
import com.aiexpenseledger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Group createGroup(String name, Long creatorId) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new LedgerValidationException("Group name is required");
        }
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + creatorId + " not found"));
        return groupRepository.save(new Group(trimmed, creator));
    }

    @Transactional(readOnly = true)
    public List<Group> listGroupsForUser(Long userId) {
        return groupRepository.findAllByMemberIdWithMembers(userId);
    }

    /** Loads a group with its members, provided {@code userId} belongs to it. */
    @Transactional(readOnly = true)
    public Group getGroupForMember(Long groupId, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group " + groupId + " not found"));
        if (!group.hasMember(userId)) {
            throw new GroupAccessDeniedException(groupId);
        }
        return group;
    }

    @Transactional
    public Group addMember(Long groupId, String email, Long actorId) {
        Group group = getGroupForMember(groupId, actorId);
        User newMember = userRepository.findByEmail(AuthService.normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No registered user with email " + email + ". Ask them to sign up first."));
        group.addMember(newMember);
        return group;
    }

    /** Authorization gate for every group-scoped operation. */
    @Transactional(readOnly = true)
    public void requireMembership(Long groupId, Long userId) {
        if (groupId == null) {
            throw new LedgerValidationException("groupId is required");
        }
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group " + groupId + " not found");
        }
        if (!groupRepository.isMember(groupId, userId)) {
            throw new GroupAccessDeniedException(groupId);
        }
    }
}
