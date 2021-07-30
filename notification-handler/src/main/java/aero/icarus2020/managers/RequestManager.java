package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class RequestManager extends Manager {

    private String assetName;
    private String orgAppName;
    private String targetType;
    private String assetId;


    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'request' event...");

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


        // Get applicant's org name
        OrganizationDao appOrg = asyncApplicantOrg.get();
        orgAppName = appOrg.getLegalname();

        // Get owners's org manager id
        OrganizationDao ownerOrg = asyncOwnerOrg.get();
        UserDao orgMan = ownerOrg.getManager();

        // Get asset's name
        AssetDao asset = asyncAsset.get();
        assetName = asset.getName();

        if (ownerOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = ownerOrg.getNotifications();

            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("contracts") == 0) {
                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        buildNotification(notification, orgMan, meta, content, "REQUEST", "New Contract Request");
                        break;
                    }
        }

        System.out.println("Processing 'request' event...Done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgAppName);
        meta.put("assetName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", assetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("senderName", orgAppName);
        content.put("assetName", assetName);
        return content;
    }

}
