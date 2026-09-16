package com.healthflow.intake.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic claimEventsTopic(){
        return new NewTopic("claim-events", 3, (short) 1);
    }
}
