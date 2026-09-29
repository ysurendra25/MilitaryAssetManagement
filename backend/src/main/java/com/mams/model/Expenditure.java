package com.mams.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Stock consumed (fired, used, written off). Expended stock leaves the base. */
@Entity
@Table(name = "expenditures")
public class Expenditure {

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
    private String reason;

    @Column(nullable = false)
    private LocalDate expendDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    public Expenditure() {
    }

    public Expenditure(Base base, EquipmentType equipmentType, int quantity,
                       String reason, LocalDate expendDate, String createdBy) {
        this.base = base;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
        this.reason = reason;
        this.expendDate = expendDate;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
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

    public String getReason() {
        return reason;
    }

    public LocalDate getExpendDate() {
        return expendDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
