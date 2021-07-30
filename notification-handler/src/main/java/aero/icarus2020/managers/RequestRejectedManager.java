package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class RequestRejectedManager extends Manager {
    private UserDao orgMan;
    private String assetName;
    private String orgName;
    private String targetType;
    private String assetId;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'request rejected' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("applicantId")) throw new MissingAttributeException("applicantId");
        CompletableFuture<OrganizationDao> asyncApplicantOrg = service.getOrganization(data.getProperties().get("applicantId"));
        if (!data.getProperties().containsKey("ownerId")) throw new MissingAttributeException("ownerId");
        CompletableFuture<OrganizationDao> asyncOwnerOrg = service.getOrganization(data.getProperties().get("ownerId"));
        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        assetId = data.getProperties().get("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(assetId);
        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");

        CompletableFuture.allOf(asyncOwnerOrg, asyncAsset, asyncApplicantOrg).join();

        // Get applicant's org manager id
        OrganizationDao appOrg = asyncApplicantOrg.get();
        orgMan = appOrg.getManager();

        // Get owners's org name
        OrganizationDao ownerOrg = asyncOwnerOrg.get();
        orgName = ownerOrg.getLegalname();

        // Get asset's name
        AssetDao asset = asyncAsset.get();
        assetName = asset.getName();

        if (appOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = appOrg.getNotifications();

            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("contracts") == 0) {
                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        buildNotification(notification, orgMan, meta, content, "REQUEST_REJECTED", "Request Rejected");
                        break;
                    }
        }

        System.out.println("Processing 'request rejected' event...Done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("senderName", orgName);
        content.put("assetName", assetName);
        return content;
    }
}
