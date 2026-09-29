package com.mams.web;

import com.mams.dto.SessionUser;
import com.mams.dto.TransferRequest;
import com.mams.dto.TransferView;
import com.mams.model.Transfer;
import com.mams.repository.TransferRepository;
import com.mams.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferRepository transfers;
    private final TransactionService transactions;

    public TransferController(TransferRepository transfers, TransactionService transactions) {
        this.transfers = transfers;
        this.transactions = transactions;
    }

    @GetMapping
    public List<TransferView> list(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) Long baseId,
                                   @RequestParam(required = false) Long equipmentTypeId,
                                   jakarta.servlet.http.HttpServletRequest request) {
        SessionUser user = SessionUser.from(request);
        final Long base;
        if ("COMMANDER".equals(user.role())) {
            base = user.baseId();
        } else {
            base = baseId;
        }
        LocalDate fromDate = from == null ? LocalDate.of(2000, 1, 1) : LocalDate.parse(from);
        LocalDate toDate = to == null ? LocalDate.of(2100, 1, 1) : LocalDate.parse(to);
        return transfers.findByTransferDateBetweenOrderByTransferDateDesc(fromDate, toDate)
                .stream()
                // a commander sees transfers to or from their base
                .filter(t -> base == null || t.getFromBase().getId().equals(base)
                        || t.getToBase().getId().equals(base))
                .filter(t -> equipmentTypeId == null
                        || t.getEquipmentType().getId().equals(equipmentTypeId))
                .map(TransferController::view)
                .toList();
    }

    @PostMapping
    public TransferView create(@RequestBody TransferRequest body,
                               jakarta.servlet.http.HttpServletRequest request) {
        Transfer transfer = transactions.createTransfer(body, SessionUser.from(request));
        return view(transfer);
    }

    static TransferView view(Transfer t) {
        return new TransferView(t.getId(), t.getTransferDate(), t.getFromBase().getName(),
                t.getToBase().getName(), t.getEquipmentType().getName(), t.getQuantity(),
                t.getReason(), t.getStatus().name(), t.getCreatedBy());
    }
}
