package com.energy.usage_service;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ENERGY_MESSAGES_QUEUE = "energy.messages";
    public static final String ENERGY_UPDATES_QUEUE = "energy.updates";

    @Bean
    public Queue energyMessagesQueue() {
        return new Queue(ENERGY_MESSAGES_QUEUE, true);
    }

    @Bean
    public Queue energyUpdatesQueue() {
        return new Queue(ENERGY_UPDATES_QUEUE, true);
    }
}

