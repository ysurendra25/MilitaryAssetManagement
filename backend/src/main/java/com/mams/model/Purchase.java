package com.mams.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
public class Purchase {

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

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    private String supplier;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    public Purchase() {
    }

    public Purchase(Base base, EquipmentType equipmentType, int quantity,
                    BigDecimal unitCost, String supplier, LocalDate purchaseDate,
                    String createdBy) {
        this.base = base;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.supplier = supplier;
        this.purchaseDate = purchaseDate;
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

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getSupplier() {
        return supplier;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
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
