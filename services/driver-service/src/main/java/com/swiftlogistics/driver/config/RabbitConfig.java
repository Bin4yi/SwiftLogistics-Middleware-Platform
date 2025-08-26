// services/driver-service/src/main/java/com/swiftlogistics/driver/config/RabbitConfig.java
package com.swiftlogistics.driver.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Value("${rabbitmq.exchanges.driver:driver.exchange}")
    private String driverExchange;

    @Value("${rabbitmq.exchanges.tracking:tracking.exchange}")
    private String trackingExchange;

    @Value("${rabbitmq.queues.driver-assignment:driver.assignment.queue}")
    private String driverAssignmentQueue;

    @Value("${rabbitmq.queues.route-update:route.update.queue}")
    private String routeUpdateQueue;

    @Value("${rabbitmq.queues.emergency-stop:emergency.stop.queue}")
    private String emergencyStopQueue;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // Exchanges
    @Bean
    public TopicExchange driverExchange() {
        return new TopicExchange(driverExchange);
    }

    @Bean
    public TopicExchange trackingExchange() {
        return new TopicExchange(trackingExchange);
    }

    // Queues
    @Bean
    public Queue driverAssignmentQueue() {
        return QueueBuilder.durable(driverAssignmentQueue).build();
    }

    @Bean
    public Queue routeUpdateQueue() {
        return QueueBuilder.durable(routeUpdateQueue).build();
    }

    @Bean
    public Queue emergencyStopQueue() {
        return QueueBuilder.durable(emergencyStopQueue).build();
    }

    // Bindings
    @Bean
    public Binding driverAssignmentBinding() {
        return BindingBuilder
                .bind(driverAssignmentQueue())
                .to(driverExchange())
                .with("driver.assignment.#");
    }

    @Bean
    public Binding routeUpdateBinding() {
        return BindingBuilder
                .bind(routeUpdateQueue())
                .to(driverExchange())
                .with("route.update.#");
    }

    @Bean
    public Binding emergencyStopBinding() {
        return BindingBuilder
                .bind(emergencyStopQueue())
                .to(driverExchange())
                .with("emergency.stop.#");
    }
}