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

import java.util.List;
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
public class DatasetDownloadCompletedManagerTest {
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
    private DatasetDownloadCompletedManager datasetDownloadCompletedManagerTest;

    private final String event_type = "DATASET_DOWNLOAD_COMPLETED";
    private final String email_subject = "Available Dataset Download Link";
    private final String recipientEmail = "test_email";
    private final String ownerID = String.valueOf(1);
    private final String datasetId = String.valueOf(86);
    private final String recipientID = String.valueOf(13);
    private final String userownerName = "Recipient Testing Organization";
    private final String assetName = "Dataset Demo";
    private final String downloadLink = "thisIsaDemodownloadLink";
    private final String expirationTimestamp = "07-07-2020";
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

        // Mocking Async Service to get asset's owner organization
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(ownerID));
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDaos.add(notificationsDao);
        ownerOrg.setPreferences(new PreferencesDao());
        ownerOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // Mocking Async Service to get asset's owner
        UserDao userDao = new UserDao();
        userDao.setId(Long.parseLong(recipientID));
        userDao.setOrganizationid(Long.parseLong(ownerID));
        userDao.setEmail(recipientEmail);
        when(service.getUser(recipientID)).thenReturn(CompletableFuture.completedFuture(userDao));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("datasetId", datasetId);
        meta.put("datasetName", assetName);
        meta.put("recipientId", recipientID);
        meta.put("downloadLink", downloadLink);
        meta.put("expirationTimestamp", expirationTimestamp);
        data.setProperties(meta);
    }

    /**
     * Create a valid event, and check that it will send notification successfully.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(ownerID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("member");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        datasetDownloadCompletedManagerTest.setData(data);
        datasetDownloadCompletedManagerTest.sendNotification();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("assetId", datasetId);
        meta.put("assetName", assetName);
        meta.put("downloadLink", downloadLink);
        meta.put("expirationTimestamp", expirationTimestamp);

        // send notification and test the published notification 
        WebNotification wn = datasetDownloadCompletedManagerTest.getWn();

        assertEquals("Notification Receiver", recipientID, String.valueOf(wn.getRecipient_id()));
        assertEquals("Published Notification", event_type, wn.getNotif_type());
        assertEquals("Published Notification", meta, wn.getMeta());


        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("downloadLink", downloadLink);
        content.put("expirationTimestamp", expirationTimestamp);

        // send notification and test the published email notification
        EmailNotification en = datasetDownloadCompletedManagerTest.getEn();

        assertEquals("Email Subject", email_subject, en.getSubject());
        assertEquals("Email Type", event_type, en.getType());
        assertEquals("Email Recipient", recipientEmail, en.getTo());
        assertEquals("Email Content", content, en.getContent());
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
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(ownerID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("manager");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        datasetDownloadCompletedManagerTest.setData(data);
        datasetDownloadCompletedManagerTest.sendNotification();

        // send notification and test the published notification
        // send notification and test the published notification
        WebNotification webNotification = datasetDownloadCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("downloadLink", downloadLink);
        content.put("expirationTimestamp", expirationTimestamp);

        // send notification and test the published email notification
        EmailNotification en = datasetDownloadCompletedManagerTest.getEn();

        assertEquals("Email Subject", email_subject, en.getSubject());
        assertEquals("Email Type", event_type, en.getType());
        assertEquals("Email Recipient", recipientEmail, en.getTo());
        assertEquals("Email Content", content, en.getContent());
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
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(ownerID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("manager");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        datasetDownloadCompletedManagerTest.setData(data);
        datasetDownloadCompletedManagerTest.sendNotification();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("assetId", datasetId);
        meta.put("assetName", assetName);
        meta.put("downloadLink", downloadLink);
        meta.put("expirationTimestamp", expirationTimestamp);

        // send notification and test the published notification 
        WebNotification wn = datasetDownloadCompletedManagerTest.getWn();

        assertEquals("Notification Receiver", recipientID, String.valueOf(wn.getRecipient_id()));
        assertEquals("Published Notification", event_type, wn.getNotif_type());
        assertEquals("Published Notification", meta, wn.getMeta());

        // send notification and test the published email notification
        EmailNotification en = datasetDownloadCompletedManagerTest.getEn();
        assertNull("Published Email", en);
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
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(ownerID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("manager");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct

        datasetDownloadCompletedManagerTest.setData(data);
        datasetDownloadCompletedManagerTest.sendNotification();

        // send notification and test the published notification 
        WebNotification webNotification = datasetDownloadCompletedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // send notification and test the published email notification
        EmailNotification en = datasetDownloadCompletedManagerTest.getEn();
        assertNull("Published Email", en);
    }

    /**
     * This test checks that it will throe a missingAttributeException, since it sends an invalid event
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
            datasetDownloadCompletedManagerTest.setData(data);
            datasetDownloadCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException ignored) {
        }
    }

    /**
     * This test checks that it will throe a missingAttributeException, since it sends multiple events that miss attributes
     *
     * @throws InterruptedException
     */
    @Test
    public void missingAttribute() throws InterruptedException {
        // set up the owner organization for this test
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(ownerID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        HashMap<String, String> meta = new HashMap<>();
        meta.put("datasetId", datasetId);
        meta.put("downloadLink", downloadLink);
        meta.put("expirationTimestamp", expirationTimestamp);
        meta.put("datasetName", assetName);
        data.setProperties(meta);

        try {
            datasetDownloadCompletedManagerTest.setData(data);
            datasetDownloadCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException ignored) {
        }

        meta = new HashMap<>();
        meta.put("recipientId", recipientID);
        meta.put("downloadLink", downloadLink);
        meta.put("expirationTimestamp", expirationTimestamp);
        meta.put("datasetName", assetName);
        data.setProperties(meta);

        try {
            datasetDownloadCompletedManagerTest.setData(data);
            datasetDownloadCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException ignored) {
        }

        meta = new HashMap<>();
        meta.put("datasetId", datasetId);
        meta.put("recipientId", recipientID);
        meta.put("downloadLink", downloadLink);
        meta.put("datasetName", assetName);
        data.setProperties(meta);

        try {
            datasetDownloadCompletedManagerTest.setData(data);
            datasetDownloadCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException ignored) {
        }

        meta = new HashMap<>();
        meta.put("datasetId", datasetId);
        meta.put("recipientId", recipientID);
        meta.put("expirationTimestamp", expirationTimestamp);
        meta.put("downloadLink", downloadLink);
        data.setProperties(meta);

        try {
            datasetDownloadCompletedManagerTest.setData(data);
            datasetDownloadCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException ignored) {
        }
    }
}
