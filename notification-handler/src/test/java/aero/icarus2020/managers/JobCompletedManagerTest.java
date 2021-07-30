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
public class JobCompletedManagerTest {
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
    private JobCompletedManager jobCompletedManagerTest;

    private final String recipient_email = "test_email";
    private final String event_type = "JOB_COMPLETED";
    private final String ownerID = String.valueOf(40);
    private final String ownerName = "ICARUS USER";
    private final String targetType = "job";
    private final String targetID = String.valueOf(3);
    private final String jobID = String.valueOf(5);
    private final String jobName = "Analytic Testing Job Name";
    private final String orgID = String.valueOf(13);
    private final String orgName = "Owner Testing Organization";

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
        user.setId(Long.parseLong(ownerID));
        user.setEmail(recipient_email);
        user.setOrganizationid(Long.parseLong(orgID));
        user.setFirstname("Joanna");
        user.setLastname("Georgiou");
        when(service.getUser(ownerID)).thenReturn(CompletableFuture.completedFuture(user));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);
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
        // set up the owner organization for this test
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(orgID));
        ownerOrg.setLegalname(orgName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("analytics");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(orgID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        jobCompletedManagerTest.setData(data);
        jobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = jobCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);

        assertEquals("Notification Receiver", ownerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // set published notification
        meta = new HashMap<>();
        meta.put("jobName", jobName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = jobCompletedManagerTest.getEn();
        String email_subject = "Analytics Job Completed";
        assertEquals("Email Subject", email_subject, emailNotification.getSubject());
        assertEquals("Email Type", event_type, emailNotification.getType());
        assertEquals("Email Recipient", recipient_email, emailNotification.getTo());
        assertEquals("Email Content", meta, emailNotification.getContent());
    }

    /**
     * Create a valid event, and check that it will only publish a Email event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoNotificationEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(orgID));
        ownerOrg.setLegalname(orgName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("analytics");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(orgID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        jobCompletedManagerTest.setData(data);
        jobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = jobCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // set published notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("jobName", jobName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = jobCompletedManagerTest.getEn();
        String email_subject = "Analytics Job Completed";
        assertEquals("Email Subject", email_subject, emailNotification.getSubject());
        assertEquals("Email Type", event_type, emailNotification.getType());
        assertEquals("Email Recipient", recipient_email, emailNotification.getTo());
        assertEquals("Email Content", meta, emailNotification.getContent());
    }

    /**
     * Create a valid event, and check that it will only publish a Notification event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoEmailEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(orgID));
        ownerOrg.setLegalname(orgName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("analytics");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(orgID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        jobCompletedManagerTest.setData(data);
        jobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = jobCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);

        assertEquals("Notification Receiver", ownerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // send notification and test the published email notification if it's null
        EmailNotification emailNotification = jobCompletedManagerTest.getEn();
        assertNull("Published Email", emailNotification);
    }

    /**
     * Create a valid event, and check that it won't publish either a Notification or a Email event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoEmailNotificationEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(orgID));
        ownerOrg.setLegalname(orgName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("analytics");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(orgID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct

        jobCompletedManagerTest.setData(data);
        jobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = jobCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // send notification and test the published email notification if it's null
        EmailNotification emailNotification = jobCompletedManagerTest.getEn();
        assertNull("Published Email", emailNotification);
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
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
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
        // set up the owner organization for this test
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(orgID));
        ownerOrg.setLegalname(orgName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("analytics");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(orgID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);
        data.setProperties(meta);

        try {
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);
        data.setProperties(meta);

        try {
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetID);
        data.setProperties(meta);

        try {
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetId", targetID);
        data.setProperties(meta);

        try {
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("jobId", jobID);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerID);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", orgID);
        meta.put("organizationName", orgName);
        meta.put("targetType", targetType);
        data.setProperties(meta);

        try {
            jobCompletedManagerTest.setData(data);
            jobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}