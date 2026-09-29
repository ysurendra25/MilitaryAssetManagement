package com.mams.web;

import com.mams.dto.AssignmentRequest;
import com.mams.dto.AssignmentView;
import com.mams.dto.SessionUser;
import com.mams.model.Assignment;
import com.mams.repository.AssignmentRepository;
import com.mams.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentRepository assignments;
    private final TransactionService transactions;

    public AssignmentController(AssignmentRepository assignments,
                                TransactionService transactions) {
        this.assignments = assignments;
        this.transactions = transactions;
    }

    @GetMapping
    public List<AssignmentView> list(@RequestParam(required = false) String from,
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
        return assignments.findByAssignedDateBetweenOrderByAssignedDateDesc(fromDate, toDate)
                .stream()
                .filter(a -> base == null || a.getBase().getId().equals(base))
                .filter(a -> equipmentTypeId == null
                        || a.getEquipmentType().getId().equals(equipmentTypeId))
                .map(AssignmentController::view)
                .toList();
    }

    @PostMapping
    public AssignmentView create(@RequestBody AssignmentRequest body,
                                 jakarta.servlet.http.HttpServletRequest request) {
        return view(transactions.createAssignment(body, SessionUser.from(request)));
    }

    @PostMapping("/{id}/return")
    public AssignmentView returnIt(@PathVariable Long id,
                                   jakarta.servlet.http.HttpServletRequest request) {
        return view(transactions.returnAssignment(id, SessionUser.from(request)));
    }

    static AssignmentView view(Assignment a) {
        return new AssignmentView(a.getId(), a.getPersonnelName(), a.getBase().getName(),
                a.getEquipmentType().getName(), a.getQuantity(), a.getAssignedDate(),
                a.getStatus().name(), a.getReturnedDate(), a.getCreatedBy());
    }
}
