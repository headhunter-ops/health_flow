package com.healthflow.intake;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthflowIntakeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthflowIntakeServiceApplication.class, args);
	}
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
