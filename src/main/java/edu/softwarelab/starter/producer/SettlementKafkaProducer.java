package edu.softwarelab.starter.producer;

import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class SettlementKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(SettlementKafkaProducer.class);

    // تاپیک توافق‌شده با تیم S6 (قابل خواندن از تنظیمات یا مقدار پیش‌فرض)
    @Value("${settlement.kafka.topics.daily-report:s6-daily-settlement-reports}")
    private String dailyReportTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * ارسال گزارش پایان روز به کافکا
     * در صورت بروز هرگونه خطا تا ۳ بار با فاصله ۲ ثانیه تلاش مجدد می‌کند (Retry)
     */
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendDailyReport(SettlementDailyReportEvent report) {
        String messageKey = report.reportId().toString();

        log.info("Starting publication of daily report to Kafka. report_id={}, business_date={}, total_records={}",
                report.reportId(), report.businessDate(), report.summary().totalCount());

        try {
            // ارسال به کافکا: کلید پیام reportId و بدنه خود آبجکت گزارش
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    dailyReportTopic,
                    messageKey,
                    report
            );

            // منتظر تایید بروکر کافکا می‌مانیم تا در صورت قطع بودن، اکولجمنت fail شود و وارد retry شویم
            SendResult<String, Object> result = future.get();

            log.info("Daily report successfully published to Kafka. report_id={}, topic={}, partition={}, offset={}",
                    report.reportId(),
                    dailyReportTopic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception ex) {
            log.error("Failed to publish daily report to Kafka. report_id={}, error={}",
                    report.reportId(), ex.getMessage(), ex);
            // پرتاب مجدد خطا تا مکانیزم Retry اسپرینگ فعال شود
            throw new RuntimeException("Error sending daily report to Kafka: " + ex.getMessage(), ex);
        }
    }

        /**
     * این متد فقط زمانی اجرا می‌شود که هر ۳ تلاش متد sendDailyReport با شکست مواجه شوند.
     */
    @Recover
    public void recover(Exception ex, SettlementDailyReportEvent report) {
        log.error("CRITICAL: Failed to publish daily report after 3 retries! report_id={}, business_date={}, cause={}",
                report.reportId(),
                report.businessDate(),
                ex.getMessage());

        
    }

    
}