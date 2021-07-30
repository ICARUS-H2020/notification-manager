package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ContractPaidManager extends Manager {

    private String orgName;
    private String assetName;

    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'contract paid' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("applicantId")) throw new MissingAttributeException("applicantId");
        CompletableFuture<OrganizationDao> asyncAppOrg = service.getOrganization(data.getProperties().get("applicantId"));
        OrganizationDao appOrg = asyncAppOrg.get();

        if (!data.getProperties().containsKey("ownerId")) throw new MissingAttributeException("ownerId");
        CompletableFuture<OrganizationDao> asyncOwnerOrg = service.getOrganization(data.getProperties().get("ownerId"));

        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(data.getProperties().get("assetId"));
        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        CompletableFuture<UserDao[]> asyncApplicantOrgUsers = service.getOrganizationUsers(data.getProperties().get("applicantId"));

        CompletableFuture.allOf(asyncOwnerOrg, asyncAsset, asyncApplicantOrgUsers).join();

        // Get owners's org name
        OrganizationDao ownerOrg = asyncOwnerOrg.get();
        orgName = ownerOrg.getLegalname();

        // Get asset's name
        AssetDao asset = asyncAsset.get();
        assetName = asset.getName();

        // Get users list of applicant org
        UserDao[] orgUsers = asyncApplicantOrgUsers.get();

        if (appOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = appOrg.getNotifications();

            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("contracts") == 0) {
                        for (int i = 0; i < orgUsers.length; i++) {

                            // Build email content
                            HashMap<String, String> content = buildContent();
                            // Build meta info
                            HashMap<String, String> meta = buildMeta();

                            buildNotification(notification, orgUsers[i], meta, content, "CONTRACT_PAID", "Contract Activated");
                            break;

                        }
                        break;
                    }
        }

        System.out.println("Processing 'contract paid' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<>();
        meta.put("senderName", orgName);
        meta.put("assetName", assetName);
        meta.put("targetType", data.getProperties().get("targetType"));
        meta.put("targetId", data.getProperties().get("assetId"));
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<>();
        content.put("senderName", orgName);
        content.put("assetName", assetName);
        return content;
    }
}
