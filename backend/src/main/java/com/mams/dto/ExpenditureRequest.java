package com.mams.dto;

import java.time.LocalDate;

public record ExpenditureRequest(Long baseId, Long equipmentTypeId, Integer quantity,
                                 String reason, LocalDate expendDate) {
}
