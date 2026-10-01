package com.aiexpenseledger.exception;

/** Raised when an authenticated user touches a group they are not a member of. */
public class GroupAccessDeniedException extends RuntimeException {

    public GroupAccessDeniedException(Long groupId) {
        super("You are not a member of group " + groupId);
    }
}
