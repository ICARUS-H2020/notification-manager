package aero.icarus2020.managers;

import aero.icarus2020.config.EmailSenderConfig;
import aero.icarus2020.config.NotificationListenerConfig;
import aero.icarus2020.config.WebsocketsSenderConfig;
import aero.icarus2020.consumers.NotificationListener;
import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import aero.icarus2020.producers.EmailSender;
import aero.icarus2020.producers.WebsocketsSender;
import aero.icarus2020.repositories.WebNotificationRepository;
import aero.icarus2020.services.AsyncService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SuggestionRejectedManagerTest {
    @MockBean
    private NotificationListener notificationListener;

    @MockBean
    private EmailSenderConfig emailSenderConfig;

    @MockBean
    private NotificationListenerConfig notificationListenerConfig;

    @MockBean
    private WebsocketsSenderConfig websocketsSenderConfig;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private AsyncService service;

    @MockBean
    private WebNotificationRepository webNotificationRepository;

    @MockBean
    private WebsocketsSender websocketSender;

    @MockBean
    private KafkaTemplate<String, WebNotification> kafkaWebsocketsTemplate;

    @MockBean
    private KafkaTemplate<String, EmailNotification> kafkaEmailTemplate;

    @MockBean
    private EmailSender emailSender;

    @Autowired
    private SuggestionRejectedManager suggestionRejectedManagerTest;

    private final String recipient_email = "test_email";
    private final String event_type = "SUGGESTION_REJECTED";
    private final String applicantId = String.valueOf(40);
    private final String orgId = String.valueOf(13);
    private final String title = "Suggestion Testing Title";
    private final String targetType = "suggestion";
    private final String targetId = String.valueOf(3);
    private NotificationEvent data;

    /**
     * Set up mock services
     *
     * @throws InterruptedException
     */
    @Before
    public void setUp() throws InterruptedException {
        // Mocking Kafka Topic: Websockets
        when(kafkaWebsocketsTemplate.getDefaultTopic()).thenReturn("websockets");
        when(kafkaWebsocketsTemplate.send(anyString(), any(WebNotification.class))).thenReturn(null);

        // Mocking Kafka Topic: Email
        when(kafkaEmailTemplate.getDefaultTopic()).thenReturn("email");
        when(kafkaEmailTemplate.send(anyString(), any(EmailNotification.class))).thenReturn(null);

        // Mocking database
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(null);

        // Mocking Async service
        UserDao user = new UserDao();
        user.setId(Long.parseLong(applicantId));
        user.setEmail(recipient_email);
        OrganizationDao organization = new OrganizationDao();
        organization.setId(Long.parseLong(orgId));
        organization.setManager(user);
        when(service.getOrganization(orgId)).thenReturn(CompletableFuture.completedFuture(organization));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("conceptTitle", title);
        meta.put("recipientId", orgId);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        data.setProperties(meta);
    }

    /**
     * Create a valid event, and check that it will publish the correct Notification and Email events.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // call sendNotification & check that that the published notification is correct
        suggestionRejectedManagerTest.setData(data);
        suggestionRejectedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = suggestionRejectedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("title", title);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);

        assertEquals("Notification Receiver", applicantId, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // set published notification
        meta = new HashMap<>();
        meta.put("title", title);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = suggestionRejectedManagerTest.getEn();
        String email_subject = "Concept Suggestion Rejected";
        assertEquals("Email Subject", email_subject, emailNotification.getSubject());
        assertEquals("Email Type", event_type, emailNotification.getType());
        assertEquals("Email Recipient", recipient_email, emailNotification.getTo());
        assertEquals("Email Content", meta, emailNotification.getContent());
    }

    /**
     * This test checks that it will throe a missingAttributeException, since it sends an invalid event
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void invalidEvent() {
        // create an invalid published event
        NotificationEvent data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("attribute", "value");
        data.setProperties(meta);

        try {
            suggestionRejectedManagerTest.setData(data);
            suggestionRejectedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }

    /**
     * This test checks that it will throe a missingAttributeException, since it sends multiple events that miss attributes
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void missingAttribute() throws InterruptedException {

        HashMap<String, String> meta = new HashMap<>();
        meta.put("title", title);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        data.setProperties(meta);

        try {
            suggestionRejectedManagerTest.setData(data);
            suggestionRejectedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
        meta = new HashMap<>();
        meta.put("title", title);
        meta.put("recipientId", orgId);
        meta.put("targetId", targetId);
        data.setProperties(meta);

        try {
            suggestionRejectedManagerTest.setData(data);
            suggestionRejectedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        meta = new HashMap<>();
        meta.put("title", title);
        meta.put("recipientId", orgId);
        meta.put("targetType", targetType);
        data.setProperties(meta);

        try {
            suggestionRejectedManagerTest.setData(data);
            suggestionRejectedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        meta = new HashMap<>();
        meta.put("targetId", targetId);
        meta.put("recipientId", orgId);
        meta.put("targetType", targetType);
        data.setProperties(meta);

        try {
            suggestionRejectedManagerTest.setData(data);
            suggestionRejectedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}