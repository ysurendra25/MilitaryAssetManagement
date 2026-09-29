package com.mams.web;

import com.mams.dto.ExpenditureRequest;
import com.mams.dto.ExpenditureView;
import com.mams.dto.SessionUser;
import com.mams.model.Expenditure;
import com.mams.repository.ExpenditureRepository;
import com.mams.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenditures")
public class ExpenditureController {

    private final ExpenditureRepository expenditures;
    private final TransactionService transactions;

    public ExpenditureController(ExpenditureRepository expenditures,
                                 TransactionService transactions) {
        this.expenditures = expenditures;
        this.transactions = transactions;
    }

    @GetMapping
    public List<ExpenditureView> list(@RequestParam(required = false) String from,
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
        return expenditures.findByExpendDateBetweenOrderByExpendDateDesc(fromDate, toDate)
                .stream()
                .filter(e -> base == null || e.getBase().getId().equals(base))
                .filter(e -> equipmentTypeId == null
                        || e.getEquipmentType().getId().equals(equipmentTypeId))
                .map(ExpenditureController::view)
                .toList();
    }

    @PostMapping
    public ExpenditureView create(@RequestBody ExpenditureRequest body,
                                  jakarta.servlet.http.HttpServletRequest request) {
        return view(transactions.createExpenditure(body, SessionUser.from(request)));
    }

    static ExpenditureView view(Expenditure e) {
        return new ExpenditureView(e.getId(), e.getExpendDate(), e.getBase().getName(),
                e.getEquipmentType().getName(), e.getQuantity(), e.getReason(),
                e.getCreatedBy());
    }
}
