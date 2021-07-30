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
public class DataCheckinJobCompletedManagerTest {

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
    private DataCheckinJobCompletedManager dataCheckinJobCompletedManagerTest;

    private final String recipient_email = "test_email";
    private final List<String> notification_list = new ArrayList<>();
    private final List<String> recipient_email_list = new ArrayList<>();
    private final String event_type = "DATA_CHECKIN_JOB_COMPLETED";
    private String visibility = "confidential";
    private final String ownerID = String.valueOf(13);
    private final String userOwnerID = String.valueOf(40);
    private final String assetID = String.valueOf(86);
    private final String ownerName = "Owner Testing Organization";
    private final String assetName = "Dataset Demo";
    private final NotificationEvent data = new NotificationEvent();

    @Before
    public void setUp() throws InterruptedException {
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
        ArrayList<CategoriesDao> asset_categories = new ArrayList<>();
        CategoriesDao asset_category = new CategoriesDao();
        asset_category.setId("0");
        asset_category.setName("Aircraft");
        asset_categories.add(asset_category);
        asset_category = new CategoriesDao();
        asset_category.setId("1");
        asset_category.setName("Airport");
        asset_categories.add(asset_category);
        asset.setCategories(asset_categories);
        when(service.getAsset(anyString())).thenReturn(CompletableFuture.completedFuture(asset));

        // Mocking getOrganization Async service
        UserDao userOwner = new UserDao();
        userOwner.setId(Long.parseLong(userOwnerID));
        userOwner.setEmail(recipient_email);
        when(service.getUser(userOwnerID)).thenReturn(CompletableFuture.completedFuture(userOwner));

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        ArrayList<String> userRoles = new ArrayList<>();
        userRoles.add("ORGANISATIONADMIN");
        UserDao[] userDaos = new UserDao[3];
        userDaos[0] = new UserDao();
        userDaos[1] = new UserDao();
        userDaos[2] = new UserDao();
        userDaos[0].setEmail(recipient_email_list.get(0));
        userDaos[0].setId(Long.parseLong(notification_list.get(0)));
        userDaos[0].setUserRoles(userRoles);
        userRoles = new ArrayList<>();
        userRoles.add("ORGANISATIONUSER");
        userDaos[1].setEmail(recipient_email_list.get(1));
        userDaos[1].setId(Long.parseLong(notification_list.get(1)));
        userDaos[1].setUserRoles(userRoles);
        userDaos[2].setEmail(recipient_email_list.get(2));
        userDaos[2].setId(Long.parseLong(notification_list.get(2)));
        userDaos[2].setUserRoles(userRoles);

        Long[] organizations = new Long[4];
        List<OrganizationDao> organizationDaoList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            organizations[i] = (long) i;
            OrganizationDao organizationDao1 = new OrganizationDao();
            organizationDao1.setId(i);
            organizationDao1.setLegalname("Demo Organization " + i);
            organizationDao1.setPreferences(new PreferencesDao());
            String target = "manager";
            if (i == 0)
                target = "1";
            else if (i == 1)
                target = "2";

            ArrayList<NotificationsDao> notificationsDaos = new ArrayList<>();
            NotificationsDao notificationsDao = new NotificationsDao();
            notificationsDao.setNotifications(true);
            notificationsDao.setEmail(true);
            ArrayList<String> categories = new ArrayList<>();
            if (i == 3) {
                notificationsDao.setNotifications(false);
                notificationsDao.setEmail(false);
                categories.add(String.valueOf(i - 1));
            }
            else categories.add(String.valueOf(i));
            notificationsDao.setLabel("datasets");
            notificationsDao.setTarget(target);
            notificationsDao.setCategories(categories);
            notificationsDaos.add(notificationsDao);
            organizationDao1.setNotifications(notificationsDaos);
            organizationDao1.setManager(userDaos[0]);
            organizationDaoList.add(organizationDao1);
            when(service.getOrganization(String.valueOf(organizations[i]))).thenReturn(CompletableFuture.completedFuture(organizationDaoList.get(i)));
            when(service.getOrganizationUsers(String.valueOf(organizations[i]))).thenReturn(CompletableFuture.completedFuture(userDaos));
        }

        when(service.getAllAuthorisecOrganizations(assetID)).thenReturn(CompletableFuture.completedFuture(organizations));

        // create a valid published event
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("visibility", visibility);
        meta.put("orgId", ownerID);
        meta.put("userOwnerId", userOwnerID);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);
    }

    @Test
    public void validConfidentialEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set owner organization for this test
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
        dataCheckinJobCompletedManagerTest.setData(data);
        dataCheckinJobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = dataCheckinJobCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetType", "data-asset");
        meta.put("senderName", ownerName);
        meta.put("dataCheckinJobName", assetName);
        meta.put("visibility", visibility);
        meta.put("targetId", assetID);

        assertEquals("Notification Receiver", userOwnerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // set published notification
        meta = new HashMap<>();
        meta.put("assetName", assetName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = dataCheckinJobCompletedManagerTest.getEn();
        String email_subject = "Data Check-in Job Completed";
        assertEquals("Email Subject", email_subject, emailNotification.getSubject());
        assertEquals("Email Type", event_type, emailNotification.getType());
        assertEquals("Email Recipient", recipient_email, emailNotification.getTo());
        assertEquals("Email Content", meta, emailNotification.getContent());
    }

    /**
     * This test checks that the generated event is correct - based that the event it will consume is valid -, and that
     * it will only generate event for an email due to the organization's configurations
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoNotificationEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set owner organization for this test
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
        dataCheckinJobCompletedManagerTest.setData(data);
        dataCheckinJobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's null
        WebNotification webNotification = dataCheckinJobCompletedManagerTest.getWn();

        assertNull("Published Notification", webNotification);

        // set published notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetName", assetName);

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = dataCheckinJobCompletedManagerTest.getEn();
        String email_subject = "Data Check-in Job Completed";
        assertEquals("Email Subject", email_subject, emailNotification.getSubject());
        assertEquals("Email Type", event_type, emailNotification.getType());
        assertEquals("Email Recipient", recipient_email, emailNotification.getTo());
        assertEquals("Email Content", meta, emailNotification.getContent());
    }

    /**
     * This test checks that the generated event is correct - based that the event it will consume is valid -, and that
     * it won't generate event for an email due to the organization's configurations
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoEmailEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set owner organization for this test
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
        dataCheckinJobCompletedManagerTest.setData(data);
        dataCheckinJobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        WebNotification webNotification = dataCheckinJobCompletedManagerTest.getWn();

        // published websocket notification
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetType", "data-asset");
        meta.put("senderName", ownerName);
        meta.put("targetId", assetID);
        meta.put("dataCheckinJobName", assetName);
        meta.put("visibility", visibility);

        assertEquals("Notification Receiver", userOwnerID, String.valueOf(webNotification.getRecipient_id()));
        assertEquals("Published Notification", event_type, webNotification.getNotif_type());
        assertEquals("Published Notification", meta, webNotification.getMeta());

        // send notification and test the published email notification if it's correct
        EmailNotification emailNotification = dataCheckinJobCompletedManagerTest.getEn();

        assertNull("Published Email Notification", emailNotification);
    }

    /**
     * This test checks that the generated event is correct - based that the event it will consume is valid -, and that
     * it won't generate event for either a notification or an email due to the organization's configurations
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validNoNotificationEmailEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set owner organization for this test
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
        dataCheckinJobCompletedManagerTest.setData(data);
        dataCheckinJobCompletedManagerTest.sendNotification();

        // send notification and test the published notification is null
        WebNotification webNotification = dataCheckinJobCompletedManagerTest.getWn();

        assertNull("Notification Receiver", webNotification);

        // send notification and test the published email notification if it's null
        EmailNotification emailNotification = dataCheckinJobCompletedManagerTest.getEn();

        assertNull("Notification Receiver", emailNotification);
    }

    /**
     * In this test we only test that the published events for the other organization (for the New Dataset event)
     * is published correctly
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MissingAttributeException
     */
    @Test
    public void validEvent() throws InterruptedException, ExecutionException, MissingAttributeException {

        // set owner organization for this test
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

        visibility = "private"; // this is the same case as public

        // call sendNotification & check that that the published notification is correct
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("visibility", visibility);
        meta.put("orgId", ownerID);
        meta.put("userOwnerId", userOwnerID);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);
        dataCheckinJobCompletedManagerTest.setData(data);
        dataCheckinJobCompletedManagerTest.sendNotification();

        // send notification and test the published notification if it's correct
        List<WebNotification> webNotificationList = dataCheckinJobCompletedManagerTest.getWebNotificationsList();

        // published websocket notification
        meta = new HashMap<>();
        meta.put("senderName", ownerName);
        meta.put("assetName", assetName);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        meta.put("visibility", visibility);

        int j = 0;
        int organization = 0;

        for (int i = 0; i < webNotificationList.size(); i++) {

            // set that for the first organization only members should get the notification
            if (i == 0) j = 1;
            else if (i == 2) {
                j = 0;
                organization++;
            } else if (i == 5) {
                j = 0;
                organization++;
            }
            String assetCategory = String.valueOf(organization);

            if (organization == 2) assetCategory = String.valueOf(organization - 1);

            if (assetCategory.compareTo("0") == 0) assetCategory = "Aircraft";
            else if (assetCategory.compareTo("1") == 0) assetCategory = "Airport";
            meta.put("assetCategory", assetCategory);

            WebNotification wn = webNotificationList.get(i);
            assertEquals("Notification Receiver", notification_list.get(j), String.valueOf(wn.getRecipient_id()));
            assertEquals("Published Notification", "DATASET_ADDED", wn.getNotif_type());
            assertEquals("Published Notification", meta, wn.getMeta());

            j++;
        }

        // set published notification
        meta = new HashMap<>();
        meta.put("assetName", assetName);
        meta.put("orgName", ownerName);

        String email_subject = "Availability of a New Data Asset";
        // send notification and test the published email notification if it's correct
        List<EmailNotification> emailNotificationList = dataCheckinJobCompletedManagerTest.getEmailNotificationsList();
        for (int i = 0; i < emailNotificationList.size(); i++) {
            // set that for the first organization only members should get the notification
            if (i == 0) j = 1;
            else if (i == 2) {
                j = 0;
                organization++;
            } else if (i == 5) {
                j = 0;
                organization++;
            }

            EmailNotification en = emailNotificationList.get(i);
            assertEquals("Email Subject", email_subject, en.getSubject());
            assertEquals("Email Type", "DATASET_ADDED", en.getType());
            assertEquals("Email Recipient", recipient_email_list.get(j), en.getTo());
            assertEquals("Email Content", meta, en.getContent());

            j++;
        }
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
            dataCheckinJobCompletedManagerTest.setData(data);
            dataCheckinJobCompletedManagerTest.sendNotification();
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
        // set owner organization for this test
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
        NotificationEvent data = new NotificationEvent();
        data.setEventType(event_type);
        HashMap<String, String> meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("visibility", visibility);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            dataCheckinJobCompletedManagerTest.setData(data);
            dataCheckinJobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("visibility", visibility);
        meta.put("userOwnerId", ownerID);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            dataCheckinJobCompletedManagerTest.setData(data);
            dataCheckinJobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("userOwnerId", ownerID);
        meta.put("targetType", "data-asset");
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            dataCheckinJobCompletedManagerTest.setData(data);
            dataCheckinJobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }

        // create an invalid published event (a missing attribute)
        data.setEventType(event_type);
        meta = new HashMap<>();
        meta.put("assetId", assetID);
        meta.put("visibility", visibility);
        meta.put("userOwnerId", ownerID);
        meta.put("targetId", assetID);
        data.setProperties(meta);

        try {
            dataCheckinJobCompletedManagerTest.setData(data);
            dataCheckinJobCompletedManagerTest.sendNotification();
            fail("Should raise a MissingAttributeException");
        } catch (MissingAttributeException | InterruptedException | ExecutionException e) {
        }
    }
}
