package com.mams.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    public enum Role { ADMIN, COMMANDER, LOGISTICS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Commanders are tied to one base; admins and logistics officers are not. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_id")
    private Base base;

    public User() {
    }

    public User(String username, String passwordHash, Role role, Base base) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.base = base;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Base getBase() {
        return base;
    }
}
