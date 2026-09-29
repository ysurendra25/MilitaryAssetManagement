package com.mams.model;

import jakarta.persistence.*;

/**
 * Quantity of each equipment type held at each base when tracking starts.
 * Every later balance is derived from this plus the transaction history,
 * so balances can never disagree with the ledger.
 */
@Entity
@Table(name = "opening_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"base_id", "equipment_type_id"}))
public class OpeningBalance {

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

    public OpeningBalance() {
    }

    public OpeningBalance(Base base, EquipmentType equipmentType, int quantity) {
        this.base = base;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
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
}
