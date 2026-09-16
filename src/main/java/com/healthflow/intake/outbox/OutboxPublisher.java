package com.healthflow.intake.outbox;

import com.healthflow.intake.entity.OutboxEvent;
import com.healthflow.intake.enums.OutboxStatus;
import com.healthflow.intake.kafka.KafkaEventPublisher;
import com.healthflow.intake.repository.OutboxEventRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final String CLAIM_EVENTS_TOPIC =
            "claim-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaEventPublisher kafkaEventPublisher) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING
                        );

        for (OutboxEvent event : events) {

            try {

                kafkaEventPublisher
                        .publish(
                                CLAIM_EVENTS_TOPIC,
                                event.getAggregateId(),
                                event.getPayload()
                        )
                        .get(10, TimeUnit.SECONDS);

                event.setStatus(
                        OutboxStatus.PUBLISHED
                );

                event.setCreatedAt(
                        LocalDateTime.now()
                );

                outboxEventRepository.save(event);

            } catch (Exception ex) {

                // Keep event PENDING.
                // Scheduler will retry it later.

                System.err.println(
                        "Failed to publish outbox event: "
                                + event.getEventId()
                );
            }
        }
    }
}