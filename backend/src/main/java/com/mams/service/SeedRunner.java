package com.mams.service;

import com.mams.model.Assignment;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Expenditure;
import com.mams.model.OpeningBalance;
import com.mams.model.Purchase;
import com.mams.model.Transfer;
import com.mams.model.User;
import com.mams.repository.AssignmentRepository;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.ExpenditureRepository;
import com.mams.repository.OpeningBalanceRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.TransferRepository;
import com.mams.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Random;

/**
 * Deterministic demo data (Random(42)): every run of a fresh database
 * produces exactly the same history, so screens, metrics and tests are
 * reproducible. Delete ./data to re-seed.
 */
@Component
public class SeedRunner implements CommandLineRunner {

    private final BaseRepository bases;
    private final EquipmentTypeRepository equipmentTypes;
    private final UserRepository users;
    private final OpeningBalanceRepository openingBalances;
    private final PurchaseRepository purchases;
    private final TransferRepository transfers;
    private final AssignmentRepository assignments;
    private final ExpenditureRepository expenditures;
    private final AuditService audit;
    private final boolean seedEnabled;

    public SeedRunner(BaseRepository bases, EquipmentTypeRepository equipmentTypes,
                      UserRepository users, OpeningBalanceRepository openingBalances,
                      PurchaseRepository purchases, TransferRepository transfers,
                      AssignmentRepository assignments, ExpenditureRepository expenditures,
                      AuditService audit,
                      @Value("${app.seed-on-empty-db:true}") boolean seedEnabled) {
        this.bases = bases;
        this.equipmentTypes = equipmentTypes;
        this.users = users;
        this.openingBalances = openingBalances;
        this.purchases = purchases;
        this.transfers = transfers;
        this.assignments = assignments;
        this.expenditures = expenditures;
        this.audit = audit;
        this.seedEnabled = seedEnabled;
    }

    @Transactional
    @Override
    public void run(String... args) {
        if (!seedEnabled || users.count() > 0) {
            return;
        }

        Random rnd = new Random(42);
        LocalDate start = LocalDate.of(2025, Month.OCTOBER, 1);
        LocalDate end = LocalDate.of(2026, Month.SEPTEMBER, 20);
        long spanDays = end.toEpochDay() - start.toEpochDay();

        Base alpha = bases.save(new Base("Base Alpha"));
        Base bravo = bases.save(new Base("Base Bravo"));
        Base charlie = bases.save(new Base("Base Charlie"));
        List<Base> allBases = List.of(alpha, bravo, charlie);

        EquipmentType weapons = equipmentTypes.save(new EquipmentType("Weapons"));
        EquipmentType vehicles = equipmentTypes.save(new EquipmentType("Vehicles"));
        EquipmentType ammunition = equipmentTypes.save(new EquipmentType("Ammunition"));
        EquipmentType gear = equipmentTypes.save(new EquipmentType("Protective Gear"));
        List<EquipmentType> allEquipment = List.of(weapons, vehicles, ammunition, gear);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        users.save(new User("admin", encoder.encode("admin123"), User.Role.ADMIN, null));
        users.save(new User("cmdr_alpha", encoder.encode("commander123"),
                User.Role.COMMANDER, alpha));
        users.save(new User("cmdr_bravo", encoder.encode("commander123"),
                User.Role.COMMANDER, bravo));
        users.save(new User("logistics01", encoder.encode("logistics123"),
                User.Role.LOGISTICS, null));

        // opening balances as of the tracking start
        int[][] opening = {
                {120, 25, 5200, 210},   // Alpha:   weapons, vehicles, ammunition, gear
                {95, 20, 4300, 180},    // Bravo
                {140, 30, 6100, 250}   // Charlie
        };
        for (int b = 0; b < allBases.size(); b++) {
            for (int e = 0; e < allEquipment.size(); e++) {
                openingBalances.save(new OpeningBalance(allBases.get(b),
                        allEquipment.get(e), opening[b][e]));
            }
        }

        String[] suppliers = {"Bharat Defence Systems", "Ordnance Factory Board",
                "Tactical Motors Pvt Ltd", "SafeGuard Industries", "Ammunition Corp of India"};
        int[][] purchaseRange = {{5, 30}, {1, 4}, {200, 1500}, {10, 60}};

        // purchases
        for (int i = 0; i < 42; i++) {
            Base base = allBases.get(rnd.nextInt(3));
            int e = rnd.nextInt(4);
            EquipmentType equipment = allEquipment.get(e);
            int qty = purchaseRange[e][0]
                    + rnd.nextInt(purchaseRange[e][1] - purchaseRange[e][0] + 1);
            LocalDate date = start.plusDays(rnd.nextInt((int) spanDays + 1));
            BigDecimal unitCost = BigDecimal.valueOf(50 + rnd.nextInt(950))
                    .multiply(BigDecimal.valueOf(e == 2 ? 9 : 1200))
                    .setScale(2, RoundingMode.HALF_UP);
            String by = rnd.nextBoolean() ? "logistics01" : "admin";
            Purchase p = new Purchase(base, equipment, qty, unitCost,
                    suppliers[rnd.nextInt(suppliers.length)], date, by);
            p.setCreatedAt(date.atTime(9 + rnd.nextInt(9), rnd.nextInt(60)));
            purchases.save(p);
            audit.log(by, by.equals("admin") ? "ADMIN" : "LOGISTICS", "POST /api/purchases",
                    equipment.getName() + " x" + qty + " for " + base.getName(), 200);
        }

        // transfers
        for (int i = 0; i < 24; i++) {
            int from = rnd.nextInt(3);
            int to = (from + 1 + rnd.nextInt(2)) % 3;
            int e = rnd.nextInt(4);
            int qty = purchaseRange[e][0] / 2 + rnd.nextInt(Math.max(2, purchaseRange[e][1] / 3));
            LocalDate date = start.plusDays(rnd.nextInt((int) spanDays + 1));
            String by = rnd.nextBoolean() ? "logistics01" : "admin";
            Transfer t = new Transfer(allBases.get(from), allBases.get(to), allEquipment.get(e),
                    Math.max(1, qty), i % 3 == 0 ? "Reinforcement" : null, date, by);
            t.setCreatedAt(date.atTime(8 + rnd.nextInt(10), rnd.nextInt(60)));
            transfers.save(t);
            audit.log(by, by.equals("admin") ? "ADMIN" : "LOGISTICS", "POST /api/transfers",
                    allEquipment.get(e).getName() + " x" + Math.max(1, qty) + ": "
                            + allBases.get(from).getName() + " -> " + allBases.get(to).getName(),
                    200);
        }

        // assignments
        String[] personnel = {"Lt. K. Mehta", "Sgt. J. Rao", "Cpl. S. Nair",
                "Maj. D. Verma", "Cpt. R. Iyer", "Pvt. A. Khan", "Sgt. M. Bose", "Lt. N. Kaur"};
        for (int i = 0; i < 18; i++) {
            Base base = allBases.get(rnd.nextInt(3));
            int e = rnd.nextInt(4);
            int qty = 1 + rnd.nextInt(Math.max(2, purchaseRange[e][1] / 4));
            LocalDate date = start.plusDays(rnd.nextInt((int) spanDays + 1));
            String by = rnd.nextBoolean() ? "admin"
                    : (base == alpha ? "cmdr_alpha" : "cmdr_bravo");
            Assignment a = new Assignment(base, allEquipment.get(e), qty,
                    personnel[rnd.nextInt(personnel.length)], date, by);
            a.setCreatedAt(date.atTime(10 + rnd.nextInt(8), rnd.nextInt(60)));
            assignments.save(a);
            audit.log(by, by.equals("admin") ? "ADMIN" : "COMMANDER", "POST /api/assignments",
                    allEquipment.get(e).getName() + " x" + qty + " to " + a.getPersonnelName()
                            + " (" + base.getName() + ")", 200);
            // three of them come back
            if (i % 6 == 5) {
                a.returnIt(date.plusWeeks(2));
                audit.log(by, by.equals("admin") ? "ADMIN" : "COMMANDER",
                        "POST /api/assignments/" + (i + 1) + "/return",
                        allEquipment.get(e).getName() + " x" + qty + " returned by "
                                + a.getPersonnelName(), 200);
            }
        }

        // expenditures
        String[] reasons = {"Quarterly range practice", "Live-fire training",
                "Border patrol deployment", "Damaged in exercise - written off",
                "Routine drills", "Expired stock disposal"};
        for (int i = 0; i < 16; i++) {
            Base base = allBases.get(rnd.nextInt(3));
            int e = rnd.nextInt(4);
            int qty = Math.max(1, purchaseRange[e][0] / 2 + rnd.nextInt(purchaseRange[e][1] / 2));
            LocalDate date = start.plusDays(rnd.nextInt((int) spanDays + 1));
            String by = rnd.nextBoolean() ? "admin"
                    : (base == alpha ? "cmdr_alpha" : "cmdr_bravo");
            Expenditure x = new Expenditure(base, allEquipment.get(e), qty,
                    reasons[rnd.nextInt(reasons.length)], date, by);
            x.setCreatedAt(date.atTime(11 + rnd.nextInt(8), rnd.nextInt(60)));
            expenditures.save(x);
            audit.log(by, by.equals("admin") ? "ADMIN" : "COMMANDER", "POST /api/expenditures",
                    allEquipment.get(e).getName() + " x" + qty + " at " + base.getName()
                            + " (" + x.getReason() + ")", 200);
        }

        audit.log("admin", "ADMIN", "SEED", "Demo data generated (deterministic, seed 42)", 200);
    }
}
