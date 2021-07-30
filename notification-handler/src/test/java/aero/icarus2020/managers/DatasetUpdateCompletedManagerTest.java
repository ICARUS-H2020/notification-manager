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
public class DatasetUpdateCompletedManagerTest {
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
    private DatasetUpdateCompletedManager datasetUpdateCompletedManagerTest;

    private final String recipient_email = "test_email";
    private final String event_type = "DATA_ASSET_UPDATE_COMPLETED";
    private final String ownerID = String.valueOf(13);
    private final String assetID = String.valueOf(86);
    private final String userOwnerID = String.valueOf(40);
    private final String ownerName = "Owner Testing Organization";
    private final String targetType = "data-asset";
    private final String assetName = "Dataset Demo";
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

        // Mocking getAsset Async service
        AssetDao asset = new AssetDao();
        asset.setId(Long.parseLong(assetID));
        asset.setName(assetName);
        OrganizationShortDao organizationDao = new OrganizationShortDao();
        organizationDao.setId(Long.parseLong(ownerID));
        organizationDao.setName(ownerName);
        asset.setOrganization(organizationDao);
        when(service.getAsset(anyString())).thenReturn(CompletableFuture.completedFuture(asset));

        // Mocking Async service
        UserDao user = new UserDao();
        user.setId(Long.parseLong(userOwnerID));
        user.setEmail(recipient_email);
        user.setOrganizationid(Long.parseLong(ownerID));
        when(service.getUser(userOwnerID)).thenReturn(CompletableFuture.completedFuture(user));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("ownerId", userOwnerID);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
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
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("data-check-in");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        datasetUpdateCompletedManagerTest.setData(data);
        datasetUpdateCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = datasetUpdateCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        assertEquals("Notification Receiver", userOwnerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // set published notification
        meta = new HashMap<>();
        meta.put("assetName", assetName);
        meta.put("orgName", ownerName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = datasetUpdateCompletedManagerTest.getEn();
        String email_subject = "Data Asset Update Completed";
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
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("data-check-in");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        datasetUpdateCompletedManagerTest.setData(data);
        datasetUpdateCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = datasetUpdateCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // set published notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetName", assetName);
        meta.put("orgName", ownerName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = datasetUpdateCompletedManagerTest.getEn();
        String email_subject = "Data Asset Update Completed";
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
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("data-check-in");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct
        datasetUpdateCompletedManagerTest.setData(data);
        datasetUpdateCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = datasetUpdateCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        assertEquals("Notification Receiver", userOwnerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // send notification and test the published email notification if it's null
        EmailNotification emailNotification = datasetUpdateCompletedManagerTest.getEn();
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
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("data-check-in");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // call sendNotification & check that that the published notification is correct

        datasetUpdateCompletedManagerTest.setData(data);
        datasetUpdateCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = datasetUpdateCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // send notification and test the published email notification if it's null
        EmailNotification emailNotification = datasetUpdateCompletedManagerTest.getEn();
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
            datasetUpdateCompletedManagerTest.setData(data);
            datasetUpdateCompletedManagerTest.sendNotification();
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
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("data-check-in");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("ownerId", userOwnerID);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            datasetUpdateCompletedManagerTest.setData(data);
            datasetUpdateCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            datasetUpdateCompletedManagerTest.setData(data);
            datasetUpdateCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("ownerId", userOwnerID);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            datasetUpdateCompletedManagerTest.setData(data);
            datasetUpdateCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}