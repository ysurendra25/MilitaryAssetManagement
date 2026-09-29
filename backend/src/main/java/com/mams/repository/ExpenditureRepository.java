package com.mams.repository;

import com.mams.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {

    List<Expenditure> findByExpendDateBetweenOrderByExpendDateDesc(
            java.time.LocalDate from, java.time.LocalDate to);
}
