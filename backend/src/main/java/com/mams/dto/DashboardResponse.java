package com.mams.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the dashboard shows. "Net Movement" is exactly the brief's
 * formula: purchases + transfers in - transfers out.
 */
public record DashboardResponse(LocalDate from, LocalDate to, Long baseId, Long equipmentTypeId,
                                long openingBalance, long closingBalance, long netMovement,
                                long purchases, long transfersIn, long transfersOut,
                                long assigned, long expended,
                                List<EquipmentRow> byEquipment,
                                List<MonthPoint> monthly) {

    /** Per-equipment-type breakdown for the filtered period. */
    public record EquipmentRow(String equipment, long opening, long purchases,
                              long transfersIn, long transfersOut, long expended,
                              long closing) {
    }

    /** One point of the monthly net-movement chart. */
    public record MonthPoint(String month, long net) {
    }
}
