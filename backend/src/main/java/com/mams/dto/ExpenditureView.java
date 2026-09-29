package com.mams.dto;

import java.time.LocalDate;

public record ExpenditureView(Long id, LocalDate date, String base, String equipment,
                              int quantity, String reason, String createdBy) {
}
