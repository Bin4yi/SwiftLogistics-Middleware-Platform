// services/integration-service/src/main/java/com/swiftlogistics/integration/config/RabbitConfig.java
package com.swiftlogistics.integration.config;

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

    @Value("${rabbitmq.exchanges.integration:integration.exchange}")
    private String integrationExchange;

    @Value("${rabbitmq.queues.order-processing:order.processing.queue}")
    private String orderProcessingQueue;

    @Value("${rabbitmq.queues.cms-processing:cms.processing.queue}")
    private String cmsProcessingQueue;

    @Value("${rabbitmq.queues.ros-processing:ros.processing.queue}")
    private String rosProcessingQueue;

    @Value("${rabbitmq.queues.wms-processing:wms.processing.queue}")
    private String wmsProcessingQueue;

    @Value("${rabbitmq.queues.integration-status:integration.status.queue}")
    private String integrationStatusQueue;

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
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }

    @Bean
    public TopicExchange integrationExchange() {
        return new TopicExchange(integrationExchange);
    }

    // Queues
    @Bean
    public Queue orderProcessingQueue() {
        return QueueBuilder.durable(orderProcessingQueue).build();
    }

    @Bean
    public Queue cmsProcessingQueue() {
        return QueueBuilder.durable(cmsProcessingQueue).build();
    }

    @Bean
    public Queue rosProcessingQueue() {
        return QueueBuilder.durable(rosProcessingQueue).build();
    }

    @Bean
    public Queue wmsProcessingQueue() {
        return QueueBuilder.durable(wmsProcessingQueue).build();
    }

    @Bean
    public Queue integrationStatusQueue() {
        return QueueBuilder.durable(integrationStatusQueue).build();
    }

    // Bindings - Order Exchange
    @Bean
    public Binding orderProcessingBinding() {
        return BindingBuilder
                .bind(orderProcessingQueue())
                .to(orderExchange())
                .with("order.process");
    }

    // Bindings - Integration Exchange
    @Bean
    public Binding cmsProcessingBinding() {
        return BindingBuilder
                .bind(cmsProcessingQueue())
                .to(integrationExchange())
                .with("cms.process");
    }

    @Bean
    public Binding rosProcessingBinding() {
        return BindingBuilder
                .bind(rosProcessingQueue())
                .to(integrationExchange())
                .with("ros.process");
    }

    @Bean
    public Binding wmsProcessingBinding() {
        return BindingBuilder
                .bind(wmsProcessingQueue())
                .to(integrationExchange())
                .with("wms.process");
    }

    @Bean
    public Binding integrationStatusBinding() {
        return BindingBuilder
                .bind(integrationStatusQueue())
                .to(integrationExchange())
                .with("integration.status.#");
    }
}