package com.mams.service;

import com.mams.dto.AssignmentRequest;
import com.mams.dto.ExpenditureRequest;
import com.mams.dto.PurchaseRequest;
import com.mams.dto.SessionUser;
import com.mams.dto.TransferRequest;
import com.mams.model.Assignment;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Expenditure;
import com.mams.model.Purchase;
import com.mams.model.Transfer;
import com.mams.repository.AssignmentRepository;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.ExpenditureRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Creates transactions. Every write is validated, scoped to the actor's role,
 * and written to the audit trail.
 */
@Service
public class TransactionService {

    private final BaseRepository bases;
    private final EquipmentTypeRepository equipmentTypes;
    private final PurchaseRepository purchases;
    private final TransferRepository transfers;
    private final AssignmentRepository assignments;
    private final ExpenditureRepository expenditures;
    private final DashboardService dashboard;
    private final AuditService audit;

    public TransactionService(BaseRepository bases, EquipmentTypeRepository equipmentTypes,
                              PurchaseRepository purchases, TransferRepository transfers,
                              AssignmentRepository assignments, ExpenditureRepository expenditures,
                              DashboardService dashboard, AuditService audit) {
        this.bases = bases;
        this.equipmentTypes = equipmentTypes;
        this.purchases = purchases;
        this.transfers = transfers;
        this.assignments = assignments;
        this.expenditures = expenditures;
        this.dashboard = dashboard;
        this.audit = audit;
    }

    @Transactional
    public Purchase createPurchase(PurchaseRequest req, SessionUser actor) {
        Base base = baseOrThrow(req.baseId());
        EquipmentType equipment = equipmentOrThrow(req.equipmentTypeId());
        requireQuantity(req.quantity());
        requireDate(req.purchaseDate());
        requireOwnBase(actor, base, "POST /api/purchases");

        Purchase purchase = purchases.save(new Purchase(base, equipment, req.quantity(),
                req.unitCost(), trim(req.supplier()), req.purchaseDate(), actor.username()));
        audit.log(actor, "POST /api/purchases",
                equipment.getName() + " x" + req.quantity() + " for " + base.getName(), 200);
        return purchase;
    }

    @Transactional
    public Transfer createTransfer(TransferRequest req, SessionUser actor) {
        Base from = baseOrThrow(req.fromBaseId());
        Base to = baseOrThrow(req.toBaseId());
        EquipmentType equipment = equipmentOrThrow(req.equipmentTypeId());
        requireQuantity(req.quantity());
        requireDate(req.transferDate());

        if (from.getId().equals(to.getId())) {
            throw bad("A transfer needs two different bases");
        }
        // a commander may only move assets to or from their own base
        if ("COMMANDER".equals(actor.role())
                && !from.getId().equals(actor.baseId()) && !to.getId().equals(actor.baseId())) {
            audit.log(actor, "POST /api/transfers",
                    "Denied: transfer does not involve the commander's base", 403);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Commanders may only transfer to or from their own base");
        }

        long stock = dashboard.stockAt(req.transferDate(), from.getId(), equipment.getId());
        if (stock < req.quantity()) {
            audit.log(actor, "POST /api/transfers",
                    "Denied: insufficient stock (" + equipment.getName() + " at " + from.getName()
                            + ": have " + stock + ", need " + req.quantity() + ")", 400);
            throw bad("Insufficient stock at " + from.getName() + ": have " + stock
                    + ", need " + req.quantity());
        }

        Transfer transfer = transfers.save(new Transfer(from, to, equipment, req.quantity(),
                trim(req.reason()), req.transferDate(), actor.username()));
        audit.log(actor, "POST /api/transfers",
                equipment.getName() + " x" + req.quantity() + ": " + from.getName()
                        + " -> " + to.getName(), 200);
        return transfer;
    }

    @Transactional
    public Assignment createAssignment(AssignmentRequest req, SessionUser actor) {
        Base base = baseOrThrow(req.baseId());
        EquipmentType equipment = equipmentOrThrow(req.equipmentTypeId());
        requireQuantity(req.quantity());
        requireDate(req.assignedDate());
        if (req.personnelName() == null || req.personnelName().isBlank()) {
            throw bad("Personnel name is required");
        }
        requireOwnBase(actor, base, "POST /api/assignments");

        // assigned stock must still exist at the base
        long stock = dashboard.stockAt(req.assignedDate(), base.getId(), equipment.getId());
        long active = assignments.findAll().stream()
                .filter(a -> a.getStatus() == Assignment.Status.ACTIVE)
                .filter(a -> a.getBase().getId().equals(base.getId()))
                .filter(a -> a.getEquipmentType().getId().equals(equipment.getId()))
                .mapToLong(Assignment::getQuantity).sum();
        if (active + req.quantity() > stock) {
            audit.log(actor, "POST /api/assignments",
                    "Denied: not enough unassigned stock (" + equipment.getName() + " at "
                            + base.getName() + ": available " + (stock - active)
                            + ", need " + req.quantity() + ")", 400);
            throw bad("Only " + (stock - active) + " unassigned " + equipment.getName()
                    + " at " + base.getName());
        }

        Assignment assignment = assignments.save(new Assignment(base, equipment, req.quantity(),
                req.personnelName().trim(), req.assignedDate(), actor.username()));
        audit.log(actor, "POST /api/assignments",
                equipment.getName() + " x" + req.quantity() + " to " + req.personnelName()
                        + " (" + base.getName() + ")", 200);
        return assignment;
    }

    @Transactional
    public Expenditure createExpenditure(ExpenditureRequest req, SessionUser actor) {
        Base base = baseOrThrow(req.baseId());
        EquipmentType equipment = equipmentOrThrow(req.equipmentTypeId());
        requireQuantity(req.quantity());
        requireDate(req.expendDate());
        if (req.reason() == null || req.reason().isBlank()) {
            throw bad("A reason is required for every expenditure");
        }
        requireOwnBase(actor, base, "POST /api/expenditures");

        long stock = dashboard.stockAt(req.expendDate(), base.getId(), equipment.getId());
        if (stock < req.quantity()) {
            audit.log(actor, "POST /api/expenditures",
                    "Denied: insufficient stock (" + equipment.getName() + " at " + base.getName()
                            + ": have " + stock + ", need " + req.quantity() + ")", 400);
            throw bad("Insufficient stock at " + base.getName() + ": have " + stock
                    + ", need " + req.quantity());
        }

        Expenditure expenditure = expenditures.save(new Expenditure(base, equipment,
                req.quantity(), req.reason().trim(), req.expendDate(), actor.username()));
        audit.log(actor, "POST /api/expenditures",
                equipment.getName() + " x" + req.quantity() + " at " + base.getName() + " ("
                        + req.reason().trim() + ")", 200);
        return expenditure;
    }

    @Transactional
    public Assignment returnAssignment(Long id, SessionUser actor) {
        Assignment assignment = assignments.findById(id)
                .orElseThrow(() -> bad("Assignment not found"));
        if (assignment.getStatus() != Assignment.Status.ACTIVE) {
            throw bad("Assignment was already returned");
        }
        if ("COMMANDER".equals(actor.role())
                && !assignment.getBase().getId().equals(actor.baseId())) {
            audit.log(actor, "POST /api/assignments/" + id + "/return",
                    "Denied: assignment belongs to another base", 403);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Commanders may only manage their own base");
        }
        assignment.returnIt(LocalDate.now());
        audit.log(actor, "POST /api/assignments/" + id + "/return",
                assignment.getEquipmentType().getName() + " x" + assignment.getQuantity()
                        + " returned by " + assignment.getPersonnelName(), 200);
        return assignment;
    }

    // ---- helpers -------------------------------------------------------------

    /** Base-level RBAC: a commander may only act on their own base. */
    private void requireOwnBase(SessionUser actor, Base base, String action) {
        if ("COMMANDER".equals(actor.role())
                && (actor.baseId() == null || !actor.baseId().equals(base.getId()))) {
            audit.log(actor, action, "Denied: base out of scope (" + base.getName() + ")", 403);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Commanders may only act on their own base");
        }
    }

    private Base baseOrThrow(Long id) {
        if (id == null) {
            throw bad("Base is required");
        }
        return bases.findById(id).orElseThrow(() -> bad("Unknown base"));
    }

    private EquipmentType equipmentOrThrow(Long id) {
        if (id == null) {
            throw bad("Equipment type is required");
        }
        return equipmentTypes.findById(id).orElseThrow(() -> bad("Unknown equipment type"));
    }

    private void requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw bad("Quantity must be a positive number");
        }
    }

    private void requireDate(LocalDate date) {
        if (date == null) {
            throw bad("Date is required");
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
