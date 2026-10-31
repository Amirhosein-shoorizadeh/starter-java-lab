package edu.softwarelab.starter.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SettlementDailyReportEvent(
        UUID reportId,
        LocalDate businessDate,
        OffsetDateTime generatedAt,
        SettlementReportSummary summary,
        List<SettlementReportItem> transactions
) {}