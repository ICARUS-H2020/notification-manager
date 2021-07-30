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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContractPaidManagerTest {
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
    private ContractPaidManager contractPaidManagerTest;

    private final List<String> recipient_email = new ArrayList<>();
    private final List<String> notification_list = new ArrayList<>();
    private final String event_type = "CONTRACT_PAID";
    private final String applicantID = String.valueOf(1);
    private final String ownerID = String.valueOf(13);
    private final String assetID = String.valueOf(86);
    private final String ownerName = "Owner Testing Organization";
    private final String assetName = "Dataset Demo";
    private final String email_subject = "Contract Activated";
    private NotificationEvent data;

    /**
     * Set up mock services
     *
     * @throws InterruptedException
     */
    @Before
    public void setUp() throws InterruptedException {

        recipient_email.add("test_email");
        recipient_email.add("demo@gmail.com");
        recipient_email.add("icarus_user@ucy.ac.cy");

        notification_list.add("1");
        notification_list.add("2");
        notification_list.add("3");

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

        // Mocking getOrganization Async service
        OrganizationDao ownerOrg = new OrganizationDao();
        ownerOrg.setId(Long.parseLong(ownerID));
        ownerOrg.setLegalname(ownerName);
        when(service.getOrganization(ownerID)).thenReturn(CompletableFuture.completedFuture(ownerOrg));

        UserDao[] userDaos = new UserDao[3];
        userDaos[0] = new UserDao();
        userDaos[1] = new UserDao();
        userDaos[2] = new UserDao();
        userDaos[0].setEmail(recipient_email.get(0));
        userDaos[0].setId(Long.parseLong(notification_list.get(0)));
        userDaos[1].setEmail(recipient_email.get(1));
        userDaos[1].setId(Long.parseLong(notification_list.get(1)));
        userDaos[2].setEmail(recipient_email.get(2));
        userDaos[2].setId(Long.parseLong(notification_list.get(2)));
        when(service.getOrganizationUsers(applicantID)).thenReturn(CompletableFuture.completedFuture(userDaos));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // create a valid published event
        data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("applicantId", applicantID);
        meta.put("ownerId", ownerID);
        meta.put("targetType", "buy-asset");
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

        // set up applicant organization for this event
        OrganizationDao applicantOrg = new OrganizationDao();
        applicantOrg.setId(Long.parseLong(applicantID));
        String applicantName = "Applicant Testing Organization";
        applicantOrg.setLegalname(applicantName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("contracts");
        notificationsDaos.add(notificationsDao);
        applicantOrg.setPreferences(new PreferencesDao());
        applicantOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(applicantID)).thenReturn(CompletableFuture.completedFuture(applicantOrg));

        // call sendNotification & check that that the published notification is correct
        contractPaidManagerTest.setData(data);
        contractPaidManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        List<WebNotification> webNotificationList = contractPaidManagerTest.getWebNotificationsList();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("targetId", assetID);
        meta.put("assetName", assetName);
        meta.put("targetType", "buy-asset");

        for (int i = 0; i < webNotificationList.size(); i++) {
            WebNotification wn = webNotificationList.get(i);
            assertEquals("Notification Receiver", notification_list.get(i), String.valueOf(wn.getRecipient_id()));
            assertEquals("Published Notification", event_type, wn.getNotif_type());
            assertEquals("Published Notification", meta, wn.getMeta());
        }
        // set published notification
        meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);

        // send notification and test the published email notification if it's correct
        List<EmailNotification> emailNotificationList = contractPaidManagerTest.getEmailNotificationsList();
        for (int i = 0; i < emailNotificationList.size(); i++) {
            EmailNotification en = emailNotificationList.get(i);
            assertEquals("Email Subject", email_subject, en.getSubject());
            assertEquals("Email Type", event_type, en.getType());
            assertEquals("Email Recipient", recipient_email.get(i), en.getTo());
            assertEquals("Email Content", meta, en.getContent());
        }
    }

    /**
     * Create a valid event, and check that it will publish only a Notification event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoEmailEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set up applicant organization for this event
        OrganizationDao applicantOrg = new OrganizationDao();
        applicantOrg.setId(Long.parseLong(applicantID));
        String applicantName = "Applicant Testing Organization";
        applicantOrg.setLegalname(applicantName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("contracts");
        notificationsDaos.add(notificationsDao);
        applicantOrg.setPreferences(new PreferencesDao());
        applicantOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(applicantID)).thenReturn(CompletableFuture.completedFuture(applicantOrg));

        // call sendNotification & check that that the published notification is correct
        contractPaidManagerTest.setData(data);
        contractPaidManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        List<WebNotification> webNotificationList = contractPaidManagerTest.getWebNotificationsList();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("targetId", assetID);
        meta.put("assetName", assetName);
        meta.put("targetType", "buy-asset");

        for (int i = 0; i < webNotificationList.size(); i++) {
            WebNotification wn = webNotificationList.get(i);
            assertEquals("Notification Receiver", notification_list.get(i), String.valueOf(wn.getRecipient_id()));
            assertEquals("Published Notification", event_type, wn.getNotif_type());
            assertEquals("Published Notification", meta, wn.getMeta());
        }

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotificationList = contractPaidManagerTest.getEmailNotificationsList();
        assertEquals("Published Email", 0, emailNotificationList.size());
    }

    /**
     * Create a valid event, and check that it will publish only an Email event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoNotificationEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set up applicant organization for this event
        OrganizationDao applicantOrg = new OrganizationDao();
        applicantOrg.setId(Long.parseLong(applicantID));
        String applicantName = "Applicant Testing Organization";
        applicantOrg.setLegalname(applicantName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("contracts");
        notificationsDaos.add(notificationsDao);
        applicantOrg.setPreferences(new PreferencesDao());
        applicantOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(applicantID)).thenReturn(CompletableFuture.completedFuture(applicantOrg));

        // call sendNotification & check that that the published notification is correct
        contractPaidManagerTest.setData(data);
        contractPaidManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        List<WebNotification> webNotificationList = contractPaidManagerTest.getWebNotificationsList();
        assertEquals("Published Notification", 0, webNotificationList.size());

        // set published notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);

        // send notification and test the published email notification if it's correct
        List<EmailNotification> emailNotificationList = contractPaidManagerTest.getEmailNotificationsList();
        for (int i = 0; i < emailNotificationList.size(); i++) {
            EmailNotification en = emailNotificationList.get(i);
            assertEquals("Email Subject", email_subject, en.getSubject());
            assertEquals("Email Type", event_type, en.getType());
            assertEquals("Email Recipient", recipient_email.get(i), en.getTo());
            assertEquals("Email Content", meta, en.getContent());
        }
    }

    /**
     * Create a valid event, and check that it won't publish either a Notification or an Email event.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoEmailNotificationEvent() throws InterruptedException, ExecutionException, MissingAttributeException {
        // set up applicant organization for this event
        OrganizationDao applicantOrg = new OrganizationDao();
        applicantOrg.setId(Long.parseLong(applicantID));
        String applicantName = "Applicant Testing Organization";
        applicantOrg.setLegalname(applicantName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(false);
        notificationsDao.setEmail(false);
        notificationsDao.setLabel("contracts");
        notificationsDaos.add(notificationsDao);
        applicantOrg.setPreferences(new PreferencesDao());
        applicantOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(applicantID)).thenReturn(CompletableFuture.completedFuture(applicantOrg));

        // call sendNotification & check that that the published notification is correct
        contractPaidManagerTest.setData(data);
        contractPaidManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        List<WebNotification> webNotificationList = contractPaidManagerTest.getWebNotificationsList();
        assertEquals("Published Notification", 0, webNotificationList.size());

        // send notification and test the published email notification if it's null
        List<EmailNotification> emailNotificationList = contractPaidManagerTest.getEmailNotificationsList();
        assertEquals("Published Email", 0, emailNotificationList.size());
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
            contractPaidManagerTest.setData(data);
            contractPaidManagerTest.sendNotification();
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

        // set up applicant organization for this event
        OrganizationDao applicantOrg = new OrganizationDao();
        applicantOrg.setId(Long.parseLong(applicantID));
        String applicantName = "Applicant Testing Organization";
        applicantOrg.setLegalname(applicantName);
        ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
        NotificationsDao notificationsDao = new NotificationsDao();
        notificationsDao.setNotifications(true);
        notificationsDao.setEmail(true);
        notificationsDao.setLabel("contracts");
        notificationsDaos.add(notificationsDao);
        applicantOrg.setPreferences(new PreferencesDao());
        applicantOrg.setNotifications(notificationsDaos);
        when(service.getOrganization(applicantID)).thenReturn(CompletableFuture.completedFuture(applicantOrg));

        // create an invalid published event (a missing attribute)
        NotificationEvent data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("applicantId", applicantID);
        meta.put("targetType", "buy-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            contractPaidManagerTest.setData(data);
            contractPaidManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("applicantId", applicantID);
        meta.put("ownerId", ownerID);
        meta.put("targetType", "buy-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            contractPaidManagerTest.setData(data);
            contractPaidManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("ownerId", ownerID);
        meta.put("targetType", "buy-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            contractPaidManagerTest.setData(data);
            contractPaidManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("applicantId", applicantID);
        meta.put("ownerId", ownerID);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            contractPaidManagerTest.setData(data);
            contractPaidManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}
