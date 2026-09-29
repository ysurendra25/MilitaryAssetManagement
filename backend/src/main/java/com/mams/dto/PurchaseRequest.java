package com.mams.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseRequest(Long baseId, Long equipmentTypeId, Integer quantity,
                              BigDecimal unitCost, String supplier, LocalDate purchaseDate) {
}
