package com.mams.service;

import com.mams.dto.DashboardResponse;
import com.mams.model.Assignment;
import com.mams.model.EquipmentType;
import com.mams.model.Expenditure;
import com.mams.model.Purchase;
import com.mams.model.Transfer;
import com.mams.repository.AssignmentRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.ExpenditureRepository;
import com.mams.repository.OpeningBalanceRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All balances are derived from one ledger: opening balances plus every
 * purchase, transfer and expenditure ever recorded. Nothing is cached or
 * double-booked, so the dashboard can never disagree with the history.
 *
 * Definitions (documented in the README):
 *   Net Movement = purchases + transfers in - transfers out   (the brief's formula)
 *   Closing      = opening + net movement - expended
 *   Assigned     = assets issued to personnel in the period (assignment does
 *                  not remove stock from the base; expenditure does)
 */
@Service
public class DashboardService {

    private final OpeningBalanceRepository openingBalances;
    private final PurchaseRepository purchases;
    private final TransferRepository transfers;
    private final ExpenditureRepository expenditures;
    private final AssignmentRepository assignments;
    private final EquipmentTypeRepository equipmentTypes;

    public DashboardService(OpeningBalanceRepository openingBalances,
                            PurchaseRepository purchases,
                            TransferRepository transfers,
                            ExpenditureRepository expenditures,
                            AssignmentRepository assignments,
                            EquipmentTypeRepository equipmentTypes) {
        this.openingBalances = openingBalances;
        this.purchases = purchases;
        this.transfers = transfers;
        this.expenditures = expenditures;
        this.assignments = assignments;
        this.equipmentTypes = equipmentTypes;
    }

    /** Stock held at a base (or everywhere) strictly before a date. */
    public long stockAt(LocalDate date, Long baseId, Long equipmentTypeId) {
        long initial = openingBalances.findAll().stream()
                .filter(ob -> match(ob.getBase().getId(), baseId))
                .filter(ob -> match(ob.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(ob -> ob.getQuantity()).sum();

        long in = purchases.findAll().stream()
                .filter(p -> p.getPurchaseDate().isBefore(date))
                .filter(p -> match(p.getBase().getId(), baseId))
                .filter(p -> match(p.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Purchase::getQuantity).sum();

        long transferredIn = transfers.findAll().stream()
                .filter(t -> t.getTransferDate().isBefore(date))
                .filter(t -> match(t.getToBase().getId(), baseId))
                .filter(t -> match(t.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Transfer::getQuantity).sum();

        long transferredOut = transfers.findAll().stream()
                .filter(t -> t.getTransferDate().isBefore(date))
                .filter(t -> match(t.getFromBase().getId(), baseId))
                .filter(t -> match(t.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Transfer::getQuantity).sum();

        long spent = expenditures.findAll().stream()
                .filter(e -> e.getExpendDate().isBefore(date))
                .filter(e -> match(e.getBase().getId(), baseId))
                .filter(e -> match(e.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Expenditure::getQuantity).sum();

        return initial + in + transferredIn - transferredOut - spent;
    }

    public DashboardResponse metrics(LocalDate from, LocalDate to, Long baseId,
                                     Long equipmentTypeId) {
        Metrics m = compute(from, to, baseId, equipmentTypeId);

        List<DashboardResponse.EquipmentRow> rows = new ArrayList<>();
        for (EquipmentType type : equipmentTypes.findAll()) {
            Metrics t = compute(from, to, baseId, type.getId());
            rows.add(new DashboardResponse.EquipmentRow(type.getName(),
                    t.opening, t.purchases, t.transfersIn, t.transfersOut,
                    t.expended, t.closing));
        }

        return new DashboardResponse(from, to, baseId, equipmentTypeId,
                m.opening, m.closing, m.net, m.purchases, m.transfersIn, m.transfersOut,
                m.assigned, m.expended, rows, monthlySeries(from, to, baseId));
    }

    private Metrics compute(LocalDate from, LocalDate to, Long baseId, Long equipmentTypeId) {
        long pIn = quantity(purchases.findAll(), to, baseId, equipmentTypeId, true);
        long tiIn = transferQuantity(transfers.findAll(), to, baseId, equipmentTypeId, true, true);
        long toIn = transferQuantity(transfers.findAll(), to, baseId, equipmentTypeId, true, false);
        long eIn = expenditureQuantity(expenditures.findAll(), to, baseId, equipmentTypeId, true);

        long opening = stockAt(from, baseId, equipmentTypeId);

        Metrics m = new Metrics();
        m.opening = opening;
        m.purchases = pIn;
        m.transfersIn = tiIn;
        m.transfersOut = toIn;
        m.expended = eIn;
        m.net = pIn + tiIn - toIn;
        m.closing = opening + m.net - eIn;
        m.assigned = assignments.findAll().stream()
                .filter(a -> !a.getAssignedDate().isBefore(from) && !a.getAssignedDate().isAfter(to))
                .filter(a -> match(a.getBase().getId(), baseId))
                .filter(a -> match(a.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Assignment::getQuantity).sum();
        return m;
    }

    private List<DashboardResponse.MonthPoint> monthlySeries(LocalDate from, LocalDate to,
                                                              Long baseId) {
        Map<YearMonth, Long> pByMonth = purchases.findAll().stream()
                .collect(Collectors.groupingBy(x -> YearMonth.from(x.getPurchaseDate()),
                        Collectors.summingLong(Purchase::getQuantity)));
        Map<YearMonth, Long> tiByMonth = transfers.findAll().stream()
                .filter(t -> match(t.getToBase().getId(), baseId))
                .collect(Collectors.groupingBy(x -> YearMonth.from(x.getTransferDate()),
                        Collectors.summingLong(Transfer::getQuantity)));
        Map<YearMonth, Long> toByMonth = transfers.findAll().stream()
                .filter(t -> match(t.getFromBase().getId(), baseId))
                .collect(Collectors.groupingBy(x -> YearMonth.from(x.getTransferDate()),
                        Collectors.summingLong(Transfer::getQuantity)));

        List<DashboardResponse.MonthPoint> points = new ArrayList<>();
        YearMonth m = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!m.isAfter(end) && points.size() < 12) {
            long net = pByMonth.getOrDefault(m, 0L) + tiByMonth.getOrDefault(m, 0L)
                    - toByMonth.getOrDefault(m, 0L);
            points.add(new DashboardResponse.MonthPoint(m.toString(), net));
            m = m.plusMonths(1);
        }
        return points;
    }

    private long quantity(List<Purchase> all, LocalDate boundary, Long baseId,
                           Long equipmentTypeId, boolean upTo) {
        return all.stream()
                .filter(x -> upTo ? !x.getPurchaseDate().isAfter(boundary)
                                  : x.getPurchaseDate().isBefore(boundary))
                .filter(x -> match(x.getBase().getId(), baseId))
                .filter(x -> match(x.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Purchase::getQuantity).sum();
    }

    private long transferQuantity(List<Transfer> all, LocalDate boundary, Long baseId,
                                  Long equipmentTypeId, boolean upTo, boolean inbound) {
        return all.stream()
                .filter(x -> upTo ? !x.getTransferDate().isAfter(boundary)
                                  : x.getTransferDate().isBefore(boundary))
                .filter(x -> match((inbound ? x.getToBase() : x.getFromBase()).getId(), baseId))
                .filter(x -> match(x.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Transfer::getQuantity).sum();
    }

    private long expenditureQuantity(List<Expenditure> all, LocalDate boundary, Long baseId,
                                     Long equipmentTypeId, boolean upTo) {
        return all.stream()
                .filter(x -> upTo ? !x.getExpendDate().isAfter(boundary)
                                  : x.getExpendDate().isBefore(boundary))
                .filter(x -> match(x.getBase().getId(), baseId))
                .filter(x -> match(x.getEquipmentType().getId(), equipmentTypeId))
                .mapToLong(Expenditure::getQuantity).sum();
    }

    private boolean match(Long actual, Long filter) {
        return filter == null || actual.equals(filter);
    }

    private static class Metrics {
        long opening;
        long closing;
        long net;
        long purchases;
        long transfersIn;
        long transfersOut;
        long assigned;
        long expended;
    }
}
