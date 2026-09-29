package com.mams.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Append-only audit trail. Every write (purchase, transfer, assignment,
 * expenditure, login) and every denied attempt is recorded here.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String action;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private int status;

    public AuditLog() {
    }

    public AuditLog(String username, String role, String action, String details, int status) {
        this.username = username;
        this.role = role;
        this.action = action;
        this.details = details;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public int getStatus() {
        return status;
    }
}
