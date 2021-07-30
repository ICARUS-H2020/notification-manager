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
public class InformDatasetUpdatedManagerTest {
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
    private InformDatasetUpdatedManager informDatasetUpdatedManagerTest;

    private final String event_type = "INFORM_DATASET_UPDATED";
    private final String email_subject = "New Data Asset Update";
    private final List<String> notification_list = new ArrayList<>();
    private final List<String> recipient_email_list = new ArrayList<>();
    private final String ownerID = String.valueOf(1);
    private final String assetID = String.valueOf(86);
    private final String recipientID = String.valueOf(13);
    private final String ownerName = "Owner Testing Organization";
    private final String userownerName = "Recipient Testing Organization";
    private final String targetType = "data-asset";
    private final String updateType = "replace";
    private final String assetName = "Dataset Demo";
    private NotificationEvent data;
    private ArrayList<String> userRoles;
    private UserDao manager = new UserDao();

    /**
     * Set up mock services
     *
     * @throws InterruptedException
     */
    @Before
    public void setUp() throws InterruptedException {

        // mocking notification id and emails
        recipient_email_list.add("test_email");
        recipient_email_list.add("demo@gmail.com");
        recipient_email_list.add("icarus_user@ucy.ac.cy");

        notification_list.add("0");
        notification_list.add("1");
        notification_list.add("2");

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
        UserDao [] users = new UserDao[3];
        for (int i = 0; i < 3; i++) {
            users[i] = new UserDao();
            users[i].setId(Long.parseLong(notification_list.get(i)));
            users[i].setEmail(recipient_email_list.get(i));
            userRoles = new ArrayList<>();
            if (i == 0)
                userRoles.add("ORGANISATIONADMIN");
            else userRoles.add("ORGANISATIONUSER");
            users[i].setUserRoles(userRoles);
            users[i].setOrganizationid(Long.parseLong(recipientID));
        }

        manager = users[0];
        when(service.getOrganizationUsers(recipientID)).thenReturn(CompletableFuture.completedFuture(users));

        // Mocking Async Service to get asset's owner organization
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("recipientId", recipientID);
        meta.put("update_type", updateType);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
        data.setProperties(meta);
    }

    /**
     * Create a valid event, and check that it will send notification only to the organization's members.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validMemberEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(recipientID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("1");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        // send notification and test the published notification if it's null
        List<WebNotification> webNotifications = informDatasetUpdatedManagerTest.getWebNotificationsList();
        int j = 1;
        for (int i = 0; i < webNotifications.size(); i ++) {
            WebNotification wn = webNotifications.get(i);
            assertEquals("Notification Receiver", notification_list.get(j), String.valueOf(wn.getRecipient_id()));
            assertEquals("Published Notification", event_type, wn.getNotif_type());
            assertEquals("Published Notification", meta, wn.getMeta());
            j++;
        }

        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("senderName", ownerName);

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        j = 1;
        for (int i = 0; i < emailNotifications.size(); i ++) {
            EmailNotification en = emailNotifications.get(i);
            assertEquals("Email Subject", email_subject, en.getSubject());
            assertEquals("Email Type", event_type, en.getType());
            assertEquals("Email Recipient", recipient_email_list.get(j), en.getTo());
            assertEquals("Email Content", content, en.getContent());
            j++;
        }
    }

    /**
     * Create a valid event, and check that it will publish Notification and email events because the user is a manger
     * and the organization's preferences has as a notification target the manager.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validManagerEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(recipientID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("manger");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = informDatasetUpdatedManagerTest.getWn();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        // send notification and test the published notification if it's null
        List<WebNotification> webNotifications = informDatasetUpdatedManagerTest.getWebNotificationsList();
        WebNotification wn = webNotifications.get(0);
        assertEquals("Send Only to 1 person", 1, webNotifications.size());
        assertEquals("Notification Receiver", notification_list.get(0), String.valueOf(wn.getRecipient_id()));
        assertEquals("Published Notification", event_type, wn.getNotif_type());
        assertEquals("Published Notification", meta, wn.getMeta());


        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("senderName", ownerName);

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        EmailNotification en = emailNotifications.get(0);
        assertEquals("Send Only to 1 person", 1, emailNotifications.size());
        assertEquals("Email Subject", email_subject, en.getSubject());
        assertEquals("Email Type", event_type, en.getType());
        assertEquals("Email Recipient", recipient_email_list.get(0), en.getTo());
        assertEquals("Email Content", content, en.getContent());
    }

    /**
     * Create a valid event, and check that it will publish the correct Notification and Email events to both the manager
     * and members of the organization.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up the owner organization for this test
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(recipientID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDao.setTarget("2");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = informDatasetUpdatedManagerTest.getWn();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        // send notification and test the published notification if it's null
        List<WebNotification> webNotifications = informDatasetUpdatedManagerTest.getWebNotificationsList();
        int j = 0;
        for (int i = 0; i < webNotifications.size(); i ++) {
            WebNotification wn = webNotifications.get(i);
            assertEquals("Notification Receiver", notification_list.get(j), String.valueOf(wn.getRecipient_id()));
            assertEquals("Published Notification", event_type, wn.getNotif_type());
            assertEquals("Published Notification", meta, wn.getMeta());
            j++;
        }

        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("senderName", ownerName);

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        j = 0;
        for (int i = 0; i < emailNotifications.size(); i ++) {
            EmailNotification en = emailNotifications.get(i);
            assertEquals("Email Subject", email_subject, en.getSubject());
            assertEquals("Email Type", event_type, en.getType());
            assertEquals("Email Recipient", recipient_email_list.get(j), en.getTo());
            assertEquals("Email Content", content, en.getContent());
            j++;
        }
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
        userOrg.setId(Long.parseLong(recipientID));
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
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        List <WebNotification> webNotifications = informDatasetUpdatedManagerTest.getWebNotificationsList();
        assertEquals("Published Notification", 0, webNotifications.size());

        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("senderName", ownerName);

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        EmailNotification en = emailNotifications.get(0);
        assertEquals("Send Only to 1 person", 1, emailNotifications.size());
        assertEquals("Email Subject", email_subject, en.getSubject());
        assertEquals("Email Type", event_type, en.getType());
        assertEquals("Email Recipient", recipient_email_list.get(0), en.getTo());
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
        userOrg.setId(Long.parseLong(recipientID));
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
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct
        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);

        // send notification and test the published notification if it's null
        List<WebNotification> webNotifications = informDatasetUpdatedManagerTest.getWebNotificationsList();
        WebNotification wn = webNotifications.get(0);
        assertEquals("Send Only to 1 person", 1, webNotifications.size());
        assertEquals("Notification Receiver", notification_list.get(0), String.valueOf(wn.getRecipient_id()));
        assertEquals("Published Notification", event_type, wn.getNotif_type());
        assertEquals("Published Notification", meta, wn.getMeta());

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        assertEquals("Published Email", 0, emailNotifications.size());
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
        userOrg.setId(Long.parseLong(recipientID));
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
        userOrg.setManager(manager);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        // call sendNotification & check that that the published notification is correct

        informDatasetUpdatedManagerTest.setData(data);
        informDatasetUpdatedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = informDatasetUpdatedManagerTest.getWn();
        assertNull("Published Notification", webNotification);

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotifications = informDatasetUpdatedManagerTest.getEmailNotificationsList();
        assertEquals("Published Email", 0, emailNotifications.size());
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
            informDatasetUpdatedManagerTest.setData(data);
            informDatasetUpdatedManagerTest.sendNotification();
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
        OrganizationDao userOrg = new OrganizationDao();
        userOrg.setId(Long.parseLong(recipientID));
        userOrg.setLegalname(userownerName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("datasets");
        notificationsDaos.add(notificationsDao);
        userOrg.setPreferences(new PreferencesDao());
        userOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(recipientID)).thenReturn(CompletableFuture.completedFuture(userOrg));

        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("recipientId", recipientID);
        meta.put("update_type", updateType);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            informDatasetUpdatedManagerTest.setData(data);
            informDatasetUpdatedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("update_type", updateType);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            informDatasetUpdatedManagerTest.setData(data);
            informDatasetUpdatedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        meta = new HashMap<>();
        meta.put("recipientId", recipientID);
        meta.put("update_type", updateType);
        meta.put("targetType", targetType);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            informDatasetUpdatedManagerTest.setData(data);
            informDatasetUpdatedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}
