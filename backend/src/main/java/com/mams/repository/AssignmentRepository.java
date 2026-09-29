package com.mams.repository;

import com.mams.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByAssignedDateBetweenOrderByAssignedDateDesc(
            java.time.LocalDate from, java.time.LocalDate to);
}
