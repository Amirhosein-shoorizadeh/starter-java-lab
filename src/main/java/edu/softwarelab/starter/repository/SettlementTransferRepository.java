package edu.softwarelab.starter.repository;

import edu.softwarelab.starter.entity.SettlementTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, UUID> {
    Optional<SettlementTransfer> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT t FROM SettlementTransfer t
        WHERE t.settledAt >= :fromDate
          AND t.settledAt < :toDate
          AND t.status IN :statuses
        ORDER BY t.settledAt ASC
    """)
    List<SettlementTransfer> findFinalizedTransfersForReport(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            @Param("statuses") Collection<String> statuses
    );
}