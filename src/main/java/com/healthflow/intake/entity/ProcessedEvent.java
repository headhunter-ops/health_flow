package com.healthflow.intake.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_event",
                        columnNames = "event_id"
                )
        }
)
@Data
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true
    )
    private String eventId;

    @Column(
            name = "processed_at",
            nullable = false
    )
    private LocalDateTime processedAt;
}
