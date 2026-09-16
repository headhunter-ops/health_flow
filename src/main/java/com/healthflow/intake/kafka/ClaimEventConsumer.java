package com.healthflow.intake.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ClaimEventConsumer {

    @KafkaListener(
            topics = "claim-events",
            groupId = "healthflow-validation-group"
    )
    public void consume(String message) {

        System.out.println(
                "Received claim event: " + message
        );
    }
}
