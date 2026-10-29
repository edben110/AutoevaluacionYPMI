package com.sem.pmiautoevaluacion.selfEvaluation.service;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SelfEvaluationClockConfiguration {
    @Bean
    public Clock selfEvaluationClock() { return Clock.system(ZoneId.of("America/Bogota")); }
}
