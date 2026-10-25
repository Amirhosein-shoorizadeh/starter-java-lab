package edu.softwarelab.starter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * وضعیت اجرای خود سرویس (Liveness)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * بررسی آمادگی سرویس و اتصال به دیتابیس PostgreSQL (Readiness) طبق الزام GEN-10
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) { // اعتبارسنجی با مهلت ۲ ثانیه
                return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "database", "UP"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                        "status", "DOWN",
                        "database", "DOWN"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "database", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }
}