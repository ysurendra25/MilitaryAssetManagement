package com.mams.web;

import com.mams.dto.AuditView;
import com.mams.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only (enforced by the AuthInterceptor). */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogs;

    public AuditController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<AuditView> list(@RequestParam(required = false) String user,
                                @RequestParam(defaultValue = "100") int limit) {
        int capped = Math.min(Math.max(limit, 1), 500);
        if (user != null && !user.isBlank()) {
            return auditLogs
                    .findByUsernameContainingIgnoreCaseOrderByTimestampDesc(user.trim(),
                            PageRequest.of(0, capped))
                    .stream()
                    .map(a -> new AuditView(a.getId(), a.getTimestamp(), a.getUsername(),
                            a.getRole(), a.getAction(), a.getDetails(), a.getStatus()))
                    .toList();
        }
        return auditLogs.findAllByOrderByTimestampDesc(PageRequest.of(0, capped))
                .stream()
                .map(a -> new AuditView(a.getId(), a.getTimestamp(), a.getUsername(),
                        a.getRole(), a.getAction(), a.getDetails(), a.getStatus()))
                .toList();
    }
}
