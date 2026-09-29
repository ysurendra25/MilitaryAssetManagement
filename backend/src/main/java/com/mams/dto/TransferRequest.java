package com.mams.dto;

import java.time.LocalDate;

public record TransferRequest(Long fromBaseId, Long toBaseId, Long equipmentTypeId,
                              Integer quantity, String reason, LocalDate transferDate) {
}
