package com.mams.repository;

import com.mams.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByTransferDateBetweenOrderByTransferDateDesc(
            java.time.LocalDate from, java.time.LocalDate to);
}
