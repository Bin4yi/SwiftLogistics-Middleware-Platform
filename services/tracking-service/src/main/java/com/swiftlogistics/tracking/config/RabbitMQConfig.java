// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/config/RabbitMQConfig.java

package com.swiftlogistics.tracking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ============== EXCHANGES ==============

    @Bean
    public TopicExchange trackingExchange() {
        return new TopicExchange("tracking.exchange");
    }

    // ============== QUEUES ==============

    @Bean
    public Queue orderStatusUpdatesQueue() {
        return QueueBuilder.durable("tracking.order.updates").build();
    }

    @Bean
    public Queue driverLocationUpdatesQueue() {
        return QueueBuilder.durable("tracking.driver.location").build();
    }

    @Bean
    public Queue deliveryStatusUpdatesQueue() {
        return QueueBuilder.durable("tracking.delivery.status").build();
    }

    @Bean
    public Queue integrationUpdatesQueue() {
        return QueueBuilder.durable("tracking.integration.updates").build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("tracking.notifications").build();
    }

    // ============== BINDINGS ==============

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder
                .bind(orderStatusUpdatesQueue())
                .to(trackingExchange())
                .with("order.status.#");
    }

    @Bean
    public Binding driverLocationBinding() {
        return BindingBuilder
                .bind(driverLocationUpdatesQueue())
                .to(trackingExchange())
                .with("driver.location.#");
    }

    @Bean
    public Binding deliveryStatusBinding() {
        return BindingBuilder
                .bind(deliveryStatusUpdatesQueue())
                .to(trackingExchange())
                .with("delivery.status.#");
    }

    @Bean
    public Binding integrationUpdatesBinding() {
        return BindingBuilder
                .bind(integrationUpdatesQueue())
                .to(trackingExchange())
                .with("integration.#");
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(trackingExchange())
                .with("notification.#");
    }

    // ============== RABBIT TEMPLATE ==============

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}