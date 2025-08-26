// services/order-service/src/main/java/com/swiftlogistics/order/config/RabbitConfig.java
package com.swiftlogistics.order.config;

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

    @Value("${rabbitmq.exchanges.order:order.exchange}")
    private String orderExchange;

    @Value("${rabbitmq.queues.order-processing:order.processing.queue}")
    private String orderProcessingQueue;

    @Value("${rabbitmq.queues.order-status-update:order.status.update.queue}")
    private String orderStatusUpdateQueue;

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

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }

    @Bean
    public Queue orderProcessingQueue() {
        return QueueBuilder.durable(orderProcessingQueue).build();
    }

    @Bean
    public Queue orderStatusUpdateQueue() {
        return QueueBuilder.durable(orderStatusUpdateQueue).build();
    }

    @Bean
    public Binding orderProcessingBinding() {
        return BindingBuilder
                .bind(orderProcessingQueue())
                .to(orderExchange())
                .with("order.process");
    }

    @Bean
    public Binding orderStatusUpdateBinding() {
        return BindingBuilder
                .bind(orderStatusUpdateQueue())
                .to(orderExchange())
                .with("order.status.update");
    }
}