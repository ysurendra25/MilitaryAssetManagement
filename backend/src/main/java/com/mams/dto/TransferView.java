package com.mams.dto;

import java.time.LocalDate;

public record TransferView(Long id, LocalDate date, String fromBase, String toBase,
                            String equipment, int quantity, String reason,
                            String status, String createdBy) {
}
