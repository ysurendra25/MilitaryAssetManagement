package com.mams.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseView(Long id, LocalDate date, String base, String equipment,
                           int quantity, BigDecimal unitCost, BigDecimal total,
                           String supplier, String createdBy) {
}
