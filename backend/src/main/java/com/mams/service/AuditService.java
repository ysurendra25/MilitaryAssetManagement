package com.mams.service;

import com.mams.dto.SessionUser;
import com.mams.model.AuditLog;
import com.mams.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes the append-only audit trail. Called after every write and on
 *  every denied or failed attempt. REQUIRES_NEW so that audit rows survive
 *  even when the business transaction they describe is rolled back. */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String role, String action, String details, int status) {
        repository.save(new AuditLog(username, role, action, details, status));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(SessionUser user, String action, String details, int status) {
        log(user.username(), user.role(), action, details, status);
    }
}
