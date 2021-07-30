package aero.icarus2020.producers;

import aero.icarus2020.models.WebNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class WebsocketsSender {

    @Value("${kafka.topic.websockets}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, WebNotification> kafkaWebsocketsTemplate;

    public void send(WebNotification data){
        Message<WebNotification> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaWebsocketsTemplate.send(message);
    }

}
