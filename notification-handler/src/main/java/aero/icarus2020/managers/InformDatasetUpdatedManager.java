package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class InformDatasetUpdatedManager extends Manager {

    private String assetName;
    private String targetType;
    private String targetId;
    private String orgName;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'dataset updated job' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("recipientId")) throw new MissingAttributeException("recipientId");
        CompletableFuture<OrganizationDao> asyncRecipient = service.getOrganization(data.getProperties().get("recipientId"));

        // Get org name
        OrganizationDao orgRecipient = asyncRecipient.get();

        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        targetId = data.getProperties().get("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(targetId);
        AssetDao asset = asyncAsset.get();

        long orgID = asset.getOrganization().getId();
        CompletableFuture<OrganizationDao> asyncOrg = service.getOrganization(String.valueOf(orgID));

        // Get org name
        OrganizationDao appOrg = asyncOrg.get();
        orgName = appOrg.getLegalname();

        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");
        assetName = asset.getName();

        if (orgRecipient.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = orgRecipient.getNotifications();

            // check if the organization wants to get this kind of notifications
            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("datasets") == 0) {
                        CompletableFuture<UserDao[]> ayncOrgUsers = service.getOrganizationUsers(data.getProperties().get("recipientId"));
                        UserDao[] orgUsers = ayncOrgUsers.get();

                        String target = notification.getTarget();

                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        if (target != null && (target.equalsIgnoreCase("2") || target.equalsIgnoreCase("1"))) {

                            for (UserDao userRecipient : orgUsers) {
                                if ((target.equalsIgnoreCase("1") && userRecipient.getUserRoles().contains("ORGANISATIONUSER")) || target.equalsIgnoreCase("2")) {
                                    buildNotification(notification, userRecipient, meta, content, "INFORM_DATASET_UPDATED", "New Data Asset Update");
                                }
                            }
                        } else {
                            buildNotification(notification, orgRecipient.getManager(), meta, content, "INFORM_DATASET_UPDATED", "New Data Asset Update");
                        }
                        return;
                    }
        }

        System.out.println("Processing 'dataset updated job ' event...done");
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
        content.put("senderName", orgName);
        return content;
    }
}
