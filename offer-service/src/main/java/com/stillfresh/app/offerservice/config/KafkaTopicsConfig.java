package com.stillfresh.app.offerservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Value("${offer.topic.offer-request:offer-request}")
    private String offerRequestTopic;

    @Value("${offer.topic.available-offers:available-offers}")
    private String availableOffersTopic;

    @Value("${offer.topic.offer-details-response:offer-details-response}")
    private String offerDetailsResponseTopic;

    @Bean
    public NewTopic offerRequestTopic() {
        return TopicBuilder.name(offerRequestTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic availableOffersTopic() {
        return TopicBuilder.name(availableOffersTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic offerDetailsResponseTopic() {
        return TopicBuilder.name(offerDetailsResponseTopic).partitions(1).replicas(1).build();
    }
}

