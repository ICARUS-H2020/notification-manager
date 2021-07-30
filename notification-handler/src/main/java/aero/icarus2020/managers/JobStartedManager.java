package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class JobStartedManager extends Manager {

    private String jobName;
    private String jobId;
    private String ownerId;
    private String ownerName;
    private Long organizationId;
    private String organizationName;
    private String targetType;
    private String targetId;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'job started' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("jobName")) throw new MissingAttributeException("jobName");
        jobName = data.getProperties().get("jobName");
        if (!data.getProperties().containsKey("jobId")) throw new MissingAttributeException("jobId");
        jobId = data.getProperties().get("jobId");
        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");
        if (!data.getProperties().containsKey("targetId")) throw new MissingAttributeException("targetId");
        targetId = data.getProperties().get("targetId");
        if (!data.getProperties().containsKey("ownerId")) throw new MissingAttributeException("ownerId");
        ownerId = data.getProperties().get("ownerId");
        // get recipient
        CompletableFuture<UserDao> asyncUser = service.getUser(data.getProperties().get("ownerId"));
        UserDao ownerUser = asyncUser.get();
        ownerName = ownerUser.getFirstname() + " " + ownerUser.getLastname();

        organizationId = ownerUser.getOrganizationid();
        CompletableFuture<OrganizationDao> asyncOrg = service.getOrganization(String.valueOf(organizationId));
        OrganizationDao appOrg = asyncOrg.get();
        organizationName = appOrg.getLegalname();

        if (appOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = appOrg.getNotifications();

            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("analytics") == 0) {
                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        buildNotification(notification, ownerUser, meta, content, "JOB_STARTED", "Analytics Job Started");
                        break;
                    }
        }


        System.out.println("Processing 'job started' event...Done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("jobId", jobId);
        meta.put("jobName", jobName);
        meta.put("ownerId", ownerId);
        meta.put("ownerName", ownerName);
        meta.put("organizationId", String.valueOf(organizationId));
        meta.put("organizationName", organizationName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("jobName", jobName);
        return content;
    }
}
