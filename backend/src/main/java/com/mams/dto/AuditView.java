package com.mams.dto;

import java.time.LocalDateTime;

public record AuditView(Long id, LocalDateTime timestamp, String username, String role,
                        String action, String details, int status) {
}
