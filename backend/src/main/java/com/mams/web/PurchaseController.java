package com.mams.web;

import com.mams.dto.PurchaseRequest;
import com.mams.dto.PurchaseView;
import com.mams.dto.SessionUser;
import com.mams.model.Purchase;
import com.mams.repository.PurchaseRepository;
import com.mams.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseRepository purchases;
    private final TransactionService transactions;

    public PurchaseController(PurchaseRepository purchases, TransactionService transactions) {
        this.purchases = purchases;
        this.transactions = transactions;
    }

    @GetMapping
    public List<PurchaseView> list(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) Long baseId,
                                   @RequestParam(required = false) Long equipmentTypeId,
                                   jakarta.servlet.http.HttpServletRequest request) {
        SessionUser user = SessionUser.from(request);
        if ("COMMANDER".equals(user.role())) {
            baseId = user.baseId();
        }
        LocalDate fromDate = from == null ? LocalDate.of(2000, 1, 1) : LocalDate.parse(from);
        LocalDate toDate = to == null ? LocalDate.of(2100, 1, 1) : LocalDate.parse(to);
        final Long base = baseId;
        return purchases.findByPurchaseDateBetweenOrderByPurchaseDateDesc(fromDate, toDate)
                .stream()
                .filter(p -> base == null || p.getBase().getId().equals(base))
                .filter(p -> equipmentTypeId == null
                        || p.getEquipmentType().getId().equals(equipmentTypeId))
                .map(PurchaseController::view)
                .toList();
    }

    @PostMapping
    public PurchaseView create(@RequestBody PurchaseRequest body,
                               jakarta.servlet.http.HttpServletRequest request) {
        Purchase purchase = transactions.createPurchase(body, SessionUser.from(request));
        return view(purchase);
    }

    static PurchaseView view(Purchase p) {
        BigDecimal total = p.getUnitCost() == null ? null
                : p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantity()));
        return new PurchaseView(p.getId(), p.getPurchaseDate(), p.getBase().getName(),
                p.getEquipmentType().getName(), p.getQuantity(), p.getUnitCost(), total,
                p.getSupplier(), p.getCreatedBy());
    }
}
