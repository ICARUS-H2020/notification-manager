package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DatasetUpdateCompletedManager extends Manager {

    private String assetName;
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
        System.out.println("Processing 'dataset update job completed' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("ownerId")) throw new MissingAttributeException("ownerId");
        CompletableFuture<UserDao> asyncuser = service.getUser(data.getProperties().get("ownerId"));

        // Get org name
        UserDao userOwner = asyncuser.get();
        long orgID = userOwner.getOrganizationid();

        CompletableFuture<OrganizationDao> asyncOrg = service.getOrganization(String.valueOf(orgID));

        // Get org name
        OrganizationDao appOrg = asyncOrg.get();
        orgName = appOrg.getLegalname();

        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        targetId = data.getProperties().get("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(targetId);
        AssetDao asset = asyncAsset.get();


        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");
        assetName = asset.getName();


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

                        buildNotification(notification, userOwner, meta, content, "DATA_ASSET_UPDATE_COMPLETED", "Data Asset Update Completed");

                        break;
                    }
        }

        System.out.println("Processing 'dataset update job completed' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("orgName", orgName);
        return content;
    }
}
