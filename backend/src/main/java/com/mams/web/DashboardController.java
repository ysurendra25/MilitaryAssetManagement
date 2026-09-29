package com.mams.web;

import com.mams.dto.DashboardResponse;
import com.mams.dto.SessionUser;
import com.mams.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public DashboardResponse dashboard(@RequestParam(required = false) String from,
                                       @RequestParam(required = false) String to,
                                       @RequestParam(required = false) Long baseId,
                                       @RequestParam(required = false) Long equipmentTypeId,
                                       jakarta.servlet.http.HttpServletRequest request) {
        SessionUser user = SessionUser.from(request);
        LocalDate fromDate = from == null ? LocalDate.of(2025, 10, 1) : LocalDate.parse(from);
        LocalDate toDate = to == null ? LocalDate.now() : LocalDate.parse(to);
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date is before start date");
        }
        // commanders always see their own base, whatever they ask for
        if ("COMMANDER".equals(user.role())) {
            baseId = user.baseId();
        }
        return dashboard.metrics(fromDate, toDate, baseId, equipmentTypeId);
    }
}
