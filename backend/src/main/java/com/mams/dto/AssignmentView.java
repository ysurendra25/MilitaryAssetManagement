package com.mams.dto;

import java.time.LocalDate;

public record AssignmentView(Long id, String personnelName, String base, String equipment,
                             int quantity, LocalDate assignedDate, String status,
                             LocalDate returnedDate, String createdBy) {
}
