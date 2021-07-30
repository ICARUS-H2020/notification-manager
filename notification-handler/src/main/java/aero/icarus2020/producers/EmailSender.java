package aero.icarus2020.producers;

import aero.icarus2020.models.EmailNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {

    @Value("${kafka.topic.email}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, EmailNotification> kafkaEmailTemplate;

    public void send(EmailNotification data){
        Message<EmailNotification> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaEmailTemplate.send(message);
    }

}
