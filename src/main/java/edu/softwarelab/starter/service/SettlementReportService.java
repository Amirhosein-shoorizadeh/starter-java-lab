package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import edu.softwarelab.starter.dto.report.SettlementReportItem;
import edu.softwarelab.starter.dto.report.SettlementReportSummary;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SettlementReportService {

    private static final ZoneId TEHRAN_ZONE = ZoneId.of("Asia/Tehran");
    private static final Set<String> FINALIZED_STATUSES = Set.of("SUCCESS", "FAILED");

    private final SettlementTransferRepository transferRepository;

    public SettlementReportService(SettlementTransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    /**
     * تولید گزارش پایان روز کاری برای یک تاریخ مشخص
     *
     * @param businessDate تاریخ روز کاری مورد نظر
     * @return ساختار نهایی گزارش آماده ارسال
     */
    @Transactional(readOnly = true)
    public SettlementDailyReportEvent generateDailyReport(LocalDate businessDate) {
        // ۱. محاسبه شروع و پایان روز بر اساس افق زمانی تهران (تبدیل به OffsetDateTime)
        OffsetDateTime fromDate = businessDate.atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();
        OffsetDateTime toDate = businessDate.plusDays(1).atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();

        // ۲. فراخوانی کوئری ریپازیتوری
        List<SettlementTransfer> transfers = transferRepository.findFinalizedTransfersForReport(
                fromDate,
                toDate,
                FINALIZED_STATUSES
        );

        // ۳. تبدیل موجودیت‌ها به DTO
        List<SettlementReportItem> items = transfers.stream()
                .map(this::mapToReportItem)
                .toList();

        // ۴. محاسبه خلاصه گزارش (شامل مدیریت حالت خالی بودن تراکنش‌ها)
        SettlementReportSummary summary = calculateSummary(items);

        // ۵. تولید شناسه پایدار گزارش (UUID v3 بر اساس تاریخ روز کاری)
        UUID reportId = generateDeterministicReportId(businessDate);

        // ۶. سرهم‌بندی شیء نهایی گزارش
        return new SettlementDailyReportEvent(
                reportId,
                businessDate,
                OffsetDateTime.now(),
                summary,
                items
        );
    }

    private SettlementReportItem mapToReportItem(SettlementTransfer transfer) {
        return new SettlementReportItem(
                transfer.getId(),
                transfer.getCorrelationId(),
                transfer.getIdempotencyKey(),
                transfer.getSourceIban(),
                transfer.getDestinationIban(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getSettlementType(),
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getSettledAt()
        );
    }

    private SettlementReportSummary calculateSummary(List<SettlementReportItem> items) {
        if (items.isEmpty()) {
            return new SettlementReportSummary(0, 0, 0, 0);
        }

        long totalCount = items.size();
        long totalAmount = 0L;
        long successCount = 0L;
        long failedCount = 0L;

        for (SettlementReportItem item : items) {
            if ("SUCCESS".equalsIgnoreCase(item.status())) {
                successCount++;
                totalAmount += (item.amount() != null ? item.amount() : 0L);
            } else if ("FAILED".equalsIgnoreCase(item.status())) {
                failedCount++;
            }
        }

        return new SettlementReportSummary(totalCount, totalAmount, successCount, failedCount);
    }

    /**
     * تولید شناسه یکتای پایدار (Deterministic UUID) بر اساس تاریخ
     * اگر گزارش یک روز مشخص چند بار تولید شود، این تابع دقیقاً همان UUID قبلی را می‌سازد
     * تا سرویس S6 بتواند گزارش تکراری را تشخیص داده و مجدداً اعمال نکند (Idempotency).
     */
    private UUID generateDeterministicReportId(LocalDate businessDate) {
        String seed = "SETTLEMENT-DAILY-REPORT:" + businessDate.toString();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}