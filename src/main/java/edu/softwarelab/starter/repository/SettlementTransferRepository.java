package edu.softwarelab.starter.repository;

import edu.softwarelab.starter.entity.SettlementTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, UUID> {
    Optional<SettlementTransfer> findByIdempotencyKey(String idempotencyKey);
}