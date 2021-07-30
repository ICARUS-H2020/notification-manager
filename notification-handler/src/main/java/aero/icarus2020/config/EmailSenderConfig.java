package aero.icarus2020.config;

import aero.icarus2020.models.EmailNotification;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EmailSenderConfig {

    @Value("${kafka.bootstrapAddress}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> emailProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, EmailNotification> emailProducerFactory() {
        return new DefaultKafkaProducerFactory<>(emailProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, EmailNotification> kafkaEmailTemplate() {
        return new KafkaTemplate<>(emailProducerFactory());
    }
}
