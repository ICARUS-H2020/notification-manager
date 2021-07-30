package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DataCheckinJobFailedManager extends Manager {

    private String dataCheckinJobName;
    private String orgName;
    private String targetType;
    private String targetId;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'data checkin job failed' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("userOwnerId")) throw new MissingAttributeException("userOwnerId");
        CompletableFuture<UserDao> asyncUser = service.getUser(data.getProperties().get("userOwnerId"));
        UserDao owner = asyncUser.get();

        CompletableFuture<OrganizationDao> asyncOrg = service.getOrganization(String.valueOf(owner.getOrganizationid()));

        // Get org name
        OrganizationDao appOrg = asyncOrg.get();
        orgName = appOrg.getLegalname();

        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");

        if (!data.getProperties().containsKey("dataCheckinJobId"))
            throw new MissingAttributeException("dataCheckinJobId");
        targetId = data.getProperties().get("dataCheckinJobId");

        if (!data.getProperties().containsKey("dataCheckinJobName"))
            throw new MissingAttributeException("dataCheckinJobName");
        dataCheckinJobName = data.getProperties().get("dataCheckinJobName");


        if (appOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = appOrg.getNotifications();

            // check if the organization wants to get this kind of notifications
            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("data-check-in") == 0) {
                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        buildNotification(notification, owner, meta, content, "DATA_CHECKIN_JOB_FAILED", "Data Check-in Job Failed");
                        break;
                    }
        }

        System.out.println("Processing 'data checkin job failed' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("dataCheckinJobName", dataCheckinJobName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("dataCheckinJobName", dataCheckinJobName);
        return content;
    }
}
