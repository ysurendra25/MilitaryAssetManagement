package com.mams.repository;

import com.mams.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPurchaseDateBetweenOrderByPurchaseDateDesc(
            java.time.LocalDate from, java.time.LocalDate to);
}
