package com.healthflow.intake.repository;

import com.healthflow.intake.entity.OutboxEvent;
import com.healthflow.intake.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    //oldest 100 PENDING events
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus);
}
