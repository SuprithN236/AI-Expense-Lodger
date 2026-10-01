package com.aiexpenseledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(@NotBlank @Size(max = 120) String name) {
}
