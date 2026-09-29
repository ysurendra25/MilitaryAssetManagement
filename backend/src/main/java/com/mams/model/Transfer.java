package com.mams.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    /** Applied in one step: stock leaves the source and arrives at the
     *  destination the moment the transfer is recorded. A two-step
     *  dispatch/receive flow is listed as future work in the README. */
    public enum Status { COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_base_id", nullable = false)
    private Base fromBase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_base_id", nullable = false)
    private Base toBase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @Column(nullable = false)
    private int quantity;

    private String reason;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.COMPLETED;

    public Transfer() {
    }

    public Transfer(Base fromBase, Base toBase, EquipmentType equipmentType,
                    int quantity, String reason, LocalDate transferDate, String createdBy) {
        this.fromBase = fromBase;
        this.toBase = toBase;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
        this.reason = reason;
        this.transferDate = transferDate;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.status = Status.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public Base getFromBase() {
        return fromBase;
    }

    public Base getToBase() {
        return toBase;
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

    public LocalDate getTransferDate() {
        return transferDate;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
