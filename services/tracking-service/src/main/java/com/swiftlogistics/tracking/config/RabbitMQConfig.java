// services/tracking-service/src/main/java/com/swiftlogistics/tracking/config/RabbitMQConfig.java
package com.swiftlogistics.tracking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchanges.tracking:tracking.exchange}")
    private String trackingExchange;

    @Value("${rabbitmq.queues.order-status:order.status.queue}")
    private String orderStatusQueue;

    @Value("${rabbitmq.queues.driver-location:driver.location.queue}")
    private String driverLocationQueue;

    @Value("${rabbitmq.queues.delivery-status:delivery.status.queue}")
    private String deliveryStatusQueue;

    @Value("${rabbitmq.queues.notifications:notifications.queue}")
    private String notificationsQueue;

    // Exchanges
    @Bean
    public TopicExchange trackingExchange() {
        return new TopicExchange(trackingExchange);
    }

    // Queues
    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(orderStatusQueue).build();
    }

    @Bean
    public Queue driverLocationQueue() {
        return QueueBuilder.durable(driverLocationQueue).build();
    }

    @Bean
    public Queue deliveryStatusQueue() {
        return QueueBuilder.durable(deliveryStatusQueue).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(notificationsQueue).build();
    }

    // Bindings
    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue())
                .to(trackingExchange())
                .with("order.status.#");
    }

    @Bean
    public Binding driverLocationBinding() {
        return BindingBuilder.bind(driverLocationQueue())
                .to(trackingExchange())
                .with("driver.location.#");
    }

    @Bean
    public Binding deliveryStatusBinding() {
        return BindingBuilder.bind(deliveryStatusQueue())
                .to(trackingExchange())
                .with("delivery.status.#");
    }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(trackingExchange())
                .with("notification.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}