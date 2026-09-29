package com.mams.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** An asset issued to a person. Assigned stock still belongs to the base. */
@Entity
@Table(name = "assignments")
public class Assignment {

    public enum Status { ACTIVE, RETURNED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_id", nullable = false)
    private Base base;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String personnelName;

    @Column(nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    private LocalDate returnedDate;

    public Assignment() {
    }

    public Assignment(Base base, EquipmentType equipmentType, int quantity,
                      String personnelName, LocalDate assignedDate, String createdBy) {
        this.base = base;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
        this.personnelName = personnelName;
        this.assignedDate = assignedDate;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.status = Status.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Base getBase() {
        return base;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getPersonnelName() {
        return personnelName;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public void returnIt(LocalDate date) {
        this.status = Status.RETURNED;
        this.returnedDate = date;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
