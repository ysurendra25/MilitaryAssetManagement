package com.mams.dto;

import java.time.LocalDate;

public record AssignmentRequest(Long baseId, Long equipmentTypeId, Integer quantity,
                                String personnelName, LocalDate assignedDate) {
}
