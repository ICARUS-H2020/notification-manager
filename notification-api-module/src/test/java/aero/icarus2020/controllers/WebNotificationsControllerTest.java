package aero.icarus2020.controllers;

import aero.icarus2020.exception.ResourceNotFoundException;
import aero.icarus2020.exception.UnauthorizedException;
import aero.icarus2020.models.WebNotification;
import aero.icarus2020.repository.WebNotificationRepository;
import aero.icarus2020.services.AsyncService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@WebMvcTest(WebNotificationsController.class)
public class WebNotificationsControllerTest {

    @MockBean
    private WebNotificationRepository webNotificationRepository;

    @MockBean
    private AsyncService service;

    @Value("${spring.profile.active}")
    private String activeProfile;

    @Autowired
    private MockMvc mvc;
    private Pageable newP;

    // testing token
    private final String token = "*";
    private final Cookie cookie = new Cookie("auth_token", token);

    /**
     * Set up mock services
     */
    @Before
    public void setUp() {
        // mock sort and pageables
        Sort sort = Sort.by(DESC, "_created_on");
        Pageable p = PageRequest.of(0, 20, sort);
        this.newP = PageRequest.of(p.getPageNumber(), p.getPageSize() * 2, p.getSort());

        // mock webNotificationRepository
        when(webNotificationRepository.save(any(WebNotification.class))).thenReturn(new WebNotification());

        // mock AyncService methods that are used from WebNotificationsController.class
        when(service.checkToken(any(String.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(service.isAssetValid(any(String.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(service.isAuthorisedOrganization(any(String.class), any(Long.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(service.getOrganizationId(any(String.class))).thenReturn(CompletableFuture.completedFuture(Long.parseLong("1")));
    }

    /**
     * This method tests that the method getWebNotificationsByRecipientId will return max twenty notifications.
     * In this case, all of the stored notifications are still valid.
     *
     * @throws InterruptedException,  when the connection is interrupted
     * @throws ExecutionException,    when the execution has stopped
     * @throws UnauthorizedException, when the token is not valid
     */
    @Test
    public void getWebNotificationsByRecipientIdAllValidTest() throws Exception {

        // mock database: Test for 1 stored notification in database
        List<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_CHECKIN_JOB_FAILED");
        webNotifications.add(w);
        Page<WebNotification> list = new PageImpl<>(webNotifications, newP, webNotifications.size());
        when(webNotificationRepository.findByRecipientId(40, newP)).thenReturn(list);

        mvc.perform(get("/api/v1/notifications")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notif_type", is(w.getNotif_type())));


        // mock database: Test for 21 stored notifications
        webNotifications = new ArrayList<>();
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "1");
        int j = 21;
        // create fake time
        long now = System.currentTimeMillis();
        Timestamp timestamp;
        for (int i = 0; i < 7; i++) {
            w = new WebNotification();
            w.set_notif_id(j);
            w.setNotif_type("DATA_CHECKIN_JOB_COMPLETED");
            w.setMeta(meta);
            timestamp = new Timestamp(now + TimeUnit.MINUTES.toMillis(j--));
            w.set_created_on(timestamp);
            webNotifications.add(w);
            w = new WebNotification();
            w.set_notif_id(j);
            timestamp = new Timestamp(now + TimeUnit.MINUTES.toMillis(j--));
            w.set_created_on(timestamp);
            w.setNotif_type("DATA_CHECKIN_JOB_FAILED");
            w.setMeta(meta);
            webNotifications.add(w);
            w = new WebNotification();
            w.set_notif_id(j);
            timestamp = new Timestamp(now + TimeUnit.MINUTES.toMillis(j--));
            w.set_created_on(timestamp);
            w.setNotif_type("DATA_ASSET_UPDATE_FAILED");
            w.setMeta(meta);
            webNotifications.add(w);
        }
        list = new PageImpl<>(webNotifications, newP, webNotifications.size());
        when(webNotificationRepository.findByRecipientId(40, newP)).thenReturn(list);

        mvc.perform(get("/api/v1/notifications")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)))
                .andExpect(jsonPath("$[0]._notif_id", is(21)))
                .andExpect(jsonPath("$[1]._notif_id", is(20)))
                .andExpect(jsonPath("$[2]._notif_id", is(19)))
                .andExpect(jsonPath("$[3]._notif_id", is(18)));
    }

    /**
     * This method tests that the method getWebNotificationsByRecipientId will return max notifTypes notifications.
     * In this case, not all of the stored notifications are valid.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void getWebNotificationsByRecipientIdSomeValidTest() throws Exception {

        // mock database: Test for 3 stored notification in database (one valid and one invalid)
        ArrayList<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("REQUEST_REJECTED");
        meta = new HashMap<>();
        meta.put("targetId", "2");
        w.setMeta(meta);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("INFORM_DATASET_UPDATED");
        meta = new HashMap<>();
        meta.put("targetId", "4");
        w.setMeta(meta);
        webNotifications.add(w);
        Page<WebNotification> list = new PageImpl<>(webNotifications, newP, webNotifications.size());
        when(webNotificationRepository.findByRecipientId(40, newP)).thenReturn(list);
        when(service.isAssetValid("2")).thenReturn(CompletableFuture.completedFuture(false));
        when(service.isAuthorisedOrganization("4", (long) 13)).thenReturn(CompletableFuture.completedFuture(false));

        if (activeProfile.equalsIgnoreCase("dev"))
            mvc.perform(get("/api/v1/notifications")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]._notif_id", is((int) w.get_notif_id())));
        else mvc.perform(get("/api/v1/notifications")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]._notif_id", is((int) w.get_notif_id())));
    }

    /**
     * This method tests that the method getUnseenWebNotificationsByRecipientId will return all of the notifications that a user has and
     * are unseen.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void getUnseenWebNotificationsByRecipientIdTest() throws Exception {

        // mock database: Test for 3 stored notification in database (one valid and one invalid)
        List<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("REQUEST_REJECTED");
        meta = new HashMap<>();
        meta.put("targetId", "2");
        w.setMeta(meta);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("INFORM_DATASET_UPDATED");
        meta = new HashMap<>();
        meta.put("targetId", "4");
        w.setMeta(meta);
        webNotifications.add(w);
        Page<WebNotification> list = new PageImpl<>(webNotifications, newP, webNotifications.size());
        when(webNotificationRepository.unseenByRecipientId(40, newP)).thenReturn(list);
        when(service.isAssetValid("2")).thenReturn(CompletableFuture.completedFuture(false));
        when(service.isAuthorisedOrganization("4", (long) 13)).thenReturn(CompletableFuture.completedFuture(false));

        if (activeProfile.equalsIgnoreCase("dev"))
            mvc.perform(get("/api/v1/notifications/unseen")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].notif_type", is("DATA_ASSET_UPDATE_COMPLETED")));
        else mvc.perform(get("/api/v1/notifications/unseen")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].notif_type", is("DATA_ASSET_UPDATE_COMPLETED")));
    }

    /**
     * This method tests that the method getUnseenWebNotificationsByRecipientId will return all of the notifications that a user has and
     * are unseen.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void getSeenWebNotificationsByRecipientIdTest() throws Exception {

        // mock database: Test for 3 stored notification in database (one valid and one invalid)
        List<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("REQUEST_REJECTED");
        meta = new HashMap<>();
        meta.put("targetId", "2");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("INFORM_DATASET_UPDATED");
        meta = new HashMap<>();
        meta.put("targetId", "4");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        Page<WebNotification> list = new PageImpl<>(webNotifications, newP, webNotifications.size());
        when(webNotificationRepository.seenByRecipientId(40, newP)).thenReturn(list);
        when(service.isAssetValid("2")).thenReturn(CompletableFuture.completedFuture(false));
        when(service.isAuthorisedOrganization("4", (long) 13)).thenReturn(CompletableFuture.completedFuture(false));

        if (activeProfile.equalsIgnoreCase("dev"))
            mvc.perform(get("/api/v1/notifications/seen")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].notif_type", is("DATA_ASSET_UPDATE_COMPLETED")));
        else
            mvc.perform(get("/api/v1/notifications/seen")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].notif_type", is("DATA_ASSET_UPDATE_COMPLETED")));
    }

    /**
     * This method tests that the method getWebNotificationById will return the notification that has that id.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void getWebNotificationByIdTest() throws Exception {

        // mock database
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        w.set_notif_id(3);
        w.setRecipient_id(40);
        when(webNotificationRepository.findByNotifId(3, 40)).thenReturn(w);

        mvc.perform(get("/api/v1/notifications/3")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._notif_id", is(3)));

    }

    /**
     * This method tests that the method setWebNotificationUnseenById will indeed set unseen the specific notification.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void setWebNotificationUnseenByIdTest() throws Exception {

        // mock database
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        w.set_notif_id(3);
        w.setRecipient_id(40);
        when(webNotificationRepository.findByNotifId(3, 40)).thenReturn(w);

        mvc.perform(put("/api/v1/notifications/3/unseen")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check if it did change the webNotification's seen_on date
        assertNull(w.getSeen_on());
    }

    /**
     * This method tests that the method setWebNotificationsUnseenByUserId will indeed set unseen all of the notifications
     * of a specific user.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void setWebNotificationsUnseenByUserIdTest() throws Exception {

        // mock database: Test for 3 stored notification in database (one valid and one invalid)
        List<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("REQUEST_REJECTED");
        meta = new HashMap<>();
        meta.put("targetId", "2");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("INFORM_DATASET_UPDATED");
        meta = new HashMap<>();
        meta.put("targetId", "4");
        w.setMeta(meta);
        w.setSeen_on(new Timestamp(System.currentTimeMillis()));
        webNotifications.add(w);
        when(webNotificationRepository.seenByRecipientId(40)).thenReturn(webNotifications);

        mvc.perform(put("/api/v1/notifications/unseen")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check if it did change the webNotifications' seen_on date
        webNotifications.forEach(wn -> {
            assertNull(wn.getSeen_on());
        });
    }

    /**
     * This method tests that the method setWebNotificationSeenById will indeed set seen the specific notification.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void setWebNotificationSeenByIdTest() throws Exception {

        // mock database
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(null);
        w.set_notif_id(3);
        w.setRecipient_id(40);
        when(webNotificationRepository.findByNotifId(3, 40)).thenReturn(w);

        mvc.perform(put("/api/v1/notifications/3/seen")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check if it did change the webNotification's seen_on date
        assertNotNull(w.getSeen_on());
    }

    /**
     * This method tests that the method setWebNotificationsSeenByUserId will indeed set seen all of the notifications
     * of a specific user.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void setWebNotificationsSeenByUserIdTest() throws Exception {

        // mock database: Test for 3 stored notification in database (one valid and one invalid)
        List<WebNotification> webNotifications = new ArrayList<>();
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.setSeen_on(null);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("REQUEST_REJECTED");
        meta = new HashMap<>();
        meta.put("targetId", "2");
        w.setMeta(meta);
        w.setSeen_on(null);
        webNotifications.add(w);
        w = new WebNotification();
        w.setNotif_type("INFORM_DATASET_UPDATED");
        meta = new HashMap<>();
        meta.put("targetId", "4");
        w.setMeta(meta);
        w.setSeen_on(null);
        webNotifications.add(w);
        when(webNotificationRepository.unseenByRecipientId(40)).thenReturn(webNotifications);

        mvc.perform(put("/api/v1/notifications/seen")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check if it did change the webNotifications' seen_on date
        webNotifications.forEach(wn -> {
            assertNotNull(wn.getSeen_on());
        });
    }

    /**
     * This method checks that all webNotificationsController methods that can raise a ResourceNotFoundException exception
     * do raise this exception on the right cases.
     */
    @Test
    public void ResourceNotFoundExceptionTest() {

        when(webNotificationRepository.findByNotifId(any(long.class), any(long.class))).thenReturn(null);

        try {
            mvc.perform(get("/api/v1/notifications/0")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Web Notification not found"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(put("/notifications/0/seen")
                    .cookie(cookie)
                    .param("page", "0")
                    .param("sort", "_created_on,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Web Notification not found"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This method tests that the method deleteWebNotification will delete the specific notification.
     *
     * @throws Exception, when the mvc perform is not successful
     */
    @Test
    public void deleteWebNotificationTest() throws Exception {

        // mock database
        WebNotification w = new WebNotification();
        w.setNotif_type("DATA_ASSET_UPDATE_COMPLETED");
        HashMap<String, String> meta = new HashMap<>();
        meta.put("targetId", "3");
        w.setMeta(meta);
        w.set_notif_id(3);
        w.setRecipient_id(40);
        when(webNotificationRepository.findByNotifId(3, 40)).thenReturn(w);

        mvc.perform(delete("/api/v1/notifications/3")
                .cookie(cookie)
                .param("page", "0")
                .param("sort", "_created_on,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check if it did try to delete the specific webNotification
        verify(webNotificationRepository).deleteById((long) 3);

    }

    /**
     * This method checks that all webNotificationsController methods are checking the token and raise an exception when
     * the token is not valid.
     **/
    @Test
    public void UnauthorizedExceptionTest() {

        try {
            mvc.perform(get("/api/v1/notifications/unseen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(get("/api/v1/notifications/seen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(get("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(get("/api/v1/notifications/0")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(put("/api/v1/notifications/0/seen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(put("/api/v1/notifications/0/unseen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(put("/api/v1/notifications/seen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(put("/api/v1/notifications/unseen")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mvc.perform(delete("/api/v1/notifications/0")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(mvcResult -> mvcResult.getResponse().getContentAsString().equals("Missing cookie 'auth_token' for method parameter of type String"));
        } catch (ResourceNotFoundException | InterruptedException | ExecutionException | UnauthorizedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

