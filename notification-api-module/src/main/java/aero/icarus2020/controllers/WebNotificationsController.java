package aero.icarus2020.controllers;

import aero.icarus2020.exception.UnauthorizedException;
import aero.icarus2020.models.WebNotification;
import aero.icarus2020.exception.ResourceNotFoundException;
import aero.icarus2020.repository.WebNotificationRepository;
import aero.icarus2020.services.AsyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Direction.DESC;

@CrossOrigin()
@RestController
@RequestMapping("/api/v1")
public class WebNotificationsController {

    @Autowired
    WebNotificationRepository webNotificationRepository;

    @Autowired
    private AsyncService service;

    @Value("${spring.profile.active}")
    private String activeProfile;

    // e.g.: /notifications?size=5&page=0&sort=_created_on,desc
    @GetMapping("/notifications")
    public ArrayList<WebNotification> getWebNotificationsByRecipientId(
            @CookieValue(value = "auth_token") String token,
            Pageable p
    ) throws InterruptedException, ExecutionException, UnauthorizedException {

        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        long organization_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
            organization_id = Long.parseLong(result.get("organization_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (organization_id == -1) {
            CompletableFuture<Long> asyncOrganization = service.getOrganizationId(String.valueOf(recipient_id));
            organization_id = asyncOrganization.get();
        }

        if (p == null) p = PageRequest.of(0, 40, Sort.by(DESC, "_created_on"));
        Pageable newP = PageRequest.of(p.getPageNumber(), p.getPageSize() * 2, p.getSort());
        // Get p notifications
        Page<WebNotification> list = webNotificationRepository.findByRecipientId(recipient_id, newP);

        // check that they are valid
        long finalOrganization_id = organization_id;

        ArrayList<WebNotification> validList = new ArrayList<>();
        if (list != null)
            list.forEach(notification -> {
                // if it's dev check notification
                if (activeProfile.equalsIgnoreCase("dev")) {
                    if (isNotificationValid(notification, finalOrganization_id) == 1) validList.add(notification);
                    // else check only for download dataset url if it's valid
                } else {
                    if (notification.getNotif_type().equalsIgnoreCase("DATASET_DOWNLOAD_COMPLETED"))
                        notification = isDownloadDatasetValid(notification);
                    validList.add(notification);
                }
            });

        return (ArrayList<WebNotification>) validList.stream().limit(20)
                .collect(Collectors.toList());
    }

    @GetMapping("/notifications/unseen")
    public ArrayList<WebNotification> getUnseenWebNotificationsByRecipientId(
            @CookieValue(value = "auth_token") String token,
            Pageable p
    ) throws InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        long organization_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
            organization_id = Long.parseLong(result.get("organization_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (organization_id == -1) {
            CompletableFuture<Long> asyncOrganization = service.getOrganizationId(String.valueOf(recipient_id));
            organization_id = asyncOrganization.get();
        }

        // get more notifications to be sure to not send only few notifications if most of them are not valid
        if (p == null) p = PageRequest.of(0, 40, Sort.by(DESC, "_created_on"));
        Pageable newP = PageRequest.of(p.getPageNumber(), p.getPageSize() * 2, p.getSort());
        // Get p notifications
        Page<WebNotification> list = webNotificationRepository.unseenByRecipientId(recipient_id, newP);

        // check that they are valid
        long finalOrganization_id = organization_id;

        ArrayList<WebNotification> validList = new ArrayList<>();

        if (list != null)
            list.forEach(notification -> {
                // if it's dev check notification
                if (activeProfile.equalsIgnoreCase("dev")) {
                    if (isNotificationValid(notification, finalOrganization_id) == 1) validList.add(notification);
                    // else check only for download dataset url if it's valid
                } else {
                    if (notification.getNotif_type().equalsIgnoreCase("DATASET_DOWNLOAD_COMPLETED"))
                        notification = isDownloadDatasetValid(notification);
                    validList.add(notification);
                }
            });

        return (ArrayList<WebNotification>) validList.stream().limit(20).collect(Collectors.toList());
    }

    @GetMapping("/notifications/seen")
    public ArrayList<WebNotification> getSeenWebNotificationsByRecipientId(
            @CookieValue(value = "auth_token") String token,
            Pageable p
    ) throws InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        long organization_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
            organization_id = Long.parseLong(result.get("organization_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (organization_id == -1) {
            CompletableFuture<Long> asyncOrganization = service.getOrganizationId(String.valueOf(recipient_id));
            organization_id = asyncOrganization.get();
        }

        if (p == null) p = PageRequest.of(0, 40, Sort.by(DESC, "_created_on"));
        Pageable newP = PageRequest.of(p.getPageNumber(), p.getPageSize() * 2, p.getSort());
        // Get p notifications
        Page<WebNotification> list = webNotificationRepository.seenByRecipientId(recipient_id, newP);

        // check that they are valid
        long finalOrganization_id = organization_id;

        ArrayList<WebNotification> validList = new ArrayList<>();
        if (list != null)
            list.forEach(notification -> {
                // if it's dev check notification
                if (activeProfile.equalsIgnoreCase("dev")) {
                    if (isNotificationValid(notification, finalOrganization_id) == 1) validList.add(notification);
                    // else check only for download dataset url if it's valid
                } else {
                    if (notification.getNotif_type().equalsIgnoreCase("DATASET_DOWNLOAD_COMPLETED"))
                        notification = isDownloadDatasetValid(notification);
                    validList.add(notification);
                }
            });

        return (ArrayList<WebNotification>) validList.stream().limit(20)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/notifications/{nid}")
    public ResponseEntity<WebNotification> getWebNotificationById(
            @CookieValue(value = "auth_token") String token,
            @PathVariable("nid") long _notif_id
    ) throws ResourceNotFoundException, InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Send Single Notification
        WebNotification wn = webNotificationRepository.findByNotifId(_notif_id, recipient_id);
        if (wn == null) {
            throw new ResourceNotFoundException("Web Notification not found");
        }
        return ResponseEntity.ok().body(wn);
    }

    @PutMapping("/notifications/{nid}/seen")
    public ResponseEntity<String> setWebNotificationSeenById(
            @CookieValue(value = "auth_token") String token,
            @PathVariable("nid") long _notif_id
    ) throws ResourceNotFoundException, InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mark single notification as seen
        WebNotification wn = webNotificationRepository.findByNotifId(_notif_id, recipient_id);
        if (wn == null) {
            throw new ResourceNotFoundException("Web Notification not found");
        }
        wn.setSeen_on(new java.sql.Timestamp(System.currentTimeMillis()));
        webNotificationRepository.save(wn);
        return new ResponseEntity<>("Web Notification marked as seen", HttpStatus.OK);
    }

    @PutMapping("/notifications/seen")
    public ResponseEntity<String> setWebNotificationsSeenByUserId(
            @CookieValue(value = "auth_token") String token
    ) throws InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mark all notifications as seen
        List<WebNotification> list = webNotificationRepository.unseenByRecipientId(recipient_id);
        for (WebNotification webNotification : list)
            webNotification.setSeen_on(new Timestamp(System.currentTimeMillis()));

        webNotificationRepository.saveAll(list);
        return new ResponseEntity<>("All Web Notifications marked as seen", HttpStatus.OK);
    }

    @PutMapping("/notifications/{nid}/unseen")
    public ResponseEntity<String> setWebNotificationUnseenById(
            @CookieValue(value = "auth_token") String token,
            @PathVariable("nid") long _notif_id
    ) throws ResourceNotFoundException, InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mark single notification as unseen
        WebNotification wn = webNotificationRepository.findByNotifId(_notif_id, recipient_id);
        if (wn == null) {
            throw new ResourceNotFoundException("Web Notification not found");
        }
        wn.setSeen_on(null);
        webNotificationRepository.save(wn);
        return new ResponseEntity<>("Web Notification marked as unseen", HttpStatus.OK);
    }

    @PutMapping("/notifications/unseen")
    public ResponseEntity<String> setWebNotificationsUnseenByUserId(
            @CookieValue(value = "auth_token") String token
    ) throws InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mark all notifications as unseen
        List<WebNotification> list = webNotificationRepository.seenByRecipientId(recipient_id);
        for (WebNotification webNotification : list)
            webNotification.setSeen_on(null);

        webNotificationRepository.saveAll(list);
        return new ResponseEntity<>("All Web Notifications marked as unseen", HttpStatus.OK);
    }

    @DeleteMapping("/notifications/{nid}")
    public ResponseEntity<String> deleteWebNotification(
            @CookieValue(value = "auth_token") String token,
            @PathVariable("nid") long _notif_id
    ) throws ResourceNotFoundException, InterruptedException, ExecutionException, UnauthorizedException {
        validateJWTToken(token);

        // Parse Token and get User Id
        long recipient_id = -1;
        try {
            HashMap<String, Object> result = decodeAndParseJWTPayload(token);
            recipient_id = Long.parseLong(result.get("user_id").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Delete single notification
        WebNotification wn = webNotificationRepository.findByNotifId(_notif_id, recipient_id);
        if (wn == null) {
            throw new ResourceNotFoundException("Web Notification not found");
        }
        webNotificationRepository.deleteById(_notif_id);
        return new ResponseEntity<>("Web Notification has been deleted", HttpStatus.OK);
    }

    public WebNotificationRepository getWebNotificationRepository() {
        return webNotificationRepository;
    }

    public AsyncService getService() {
        return service;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    private int isNotificationValid(WebNotification notification, long finalOrganization_id) {

        int valid = 0;
        JSONObject meta = new JSONObject(notification.getMeta());
        Boolean response = null;
        //TODO: check that job analytics exist

        switch (notification.getNotif_type()) {
            case "DATA_CHECKIN_JOB_COMPLETED":
            case "DATA_ASSET_UPDATE_COMPLETED":
            case "DATA_ASSET_UPDATE_FAILED":
            case "CONTRACT_OFFERED":
            case "CONTRACT_ACCEPTED":
            case "CONTRACT_REJECTED":
            case "CONTRACT_PAID":
            case "CONTRACT_NEGOTIATED":
            case "CONTRACT_COUNTERED":
            case "CONTRACT_OFFER_ACCEPTED":
            case "CONTRACT_OFFER_REJECTED":
            case "REQUEST":
            case "REQUEST_REJECTED":
                if (meta.has("targetId")) {
                    CompletableFuture<Boolean> asyncResponse = service.isAssetValid(meta.get("targetId").toString());
                    response = null;
                    try {
                        response = asyncResponse.get();
                        if (response) valid = 1;
                    } catch (InterruptedException | ExecutionException ignored) {
                    }
                }
                break;
            case "INFORM_DATASET_UPDATED":
            case "DATASET_ADDED":
                if (meta.has("targetId")) {
                    CompletableFuture<Boolean> asyncReseponse = service.isAssetValid(meta.get("targetId").toString());
                    response = null;
                    try {
                        response = asyncReseponse.get();
                        if (response) {
                            asyncReseponse = service.isAuthorisedOrganization(meta.get("targetId").toString(), finalOrganization_id);
                            response = asyncReseponse.get();
                            if (response) valid = 1;
                        }

                    } catch (InterruptedException | ExecutionException ignored) {
                    }
                }
                break;
            case "DATASET_DOWNLOAD_COMPLETED":
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                if (meta.has("expirationTimestamp") && Timestamp.valueOf((String) meta.get("expirationTimestamp")).after(timestamp))
                    notification.setNotif_type("DATASET_DOWNLOAD_COMPLETED_VALID");
                else notification.setNotif_type("DATASET_DOWNLOAD_COMPLETED_INVALID");
                valid = 1;
                break;
            case "DATASET_DOWNLOAD_FAILED":
                if (meta.has("assetId")) {
                    CompletableFuture<Boolean> asyncResponse = service.isAssetValid(meta.get("assetId").toString());
                    response = null;
                    try {
                        response = asyncResponse.get();
                        if (response) valid = 1;
                    } catch (InterruptedException | ExecutionException ignored) {
                    }
                }
                break;
            default:
                valid = 1;
                break;
        }
        return valid;
    }

    private WebNotification isDownloadDatasetValid(WebNotification notification) {
        JSONObject meta = new JSONObject(notification.getMeta());

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (meta.has("expirationTimestamp") && Timestamp.valueOf((String) meta.get("expirationTimestamp")).after(timestamp))
            notification.setNotif_type("DATASET_DOWNLOAD_COMPLETED_VALID");
        else notification.setNotif_type("DATASET_DOWNLOAD_COMPLETED_INVALID");

        return notification;
    }

    private HashMap decodeAndParseJWTPayload(String token) throws IOException {
        // Decode JWT payload
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        String[] parts = token.split("\\.");
        String payload = new String(decoder.decode(parts[1]));
        return new ObjectMapper().readValue(payload, HashMap.class);
    }

    private void validateJWTToken(String token) throws UnauthorizedException, ExecutionException, InterruptedException {
        CompletableFuture<Boolean> asyncCheckToken = service.checkToken(token);
        if (asyncCheckToken == null || !asyncCheckToken.get()) {
            throw new UnauthorizedException("Unauthorized");
        }
    }
}
