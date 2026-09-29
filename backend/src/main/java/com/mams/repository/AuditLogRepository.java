package com.mams.repository;

import com.mams.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsernameContainingIgnoreCaseOrderByTimestampDesc(String username, Pageable pageable);

    List<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
