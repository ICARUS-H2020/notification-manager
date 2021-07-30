package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ContractOfferAcceptedManager extends Manager {

    private UserDao orgMan;
    private String assetName;
    private String orgName;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'contract offer accepted' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("applicantId")) throw new MissingAttributeException("applicantId");
        CompletableFuture<OrganizationDao> asyncApplicantOrg = service.getOrganization(data.getProperties().get("applicantId"));
        OrganizationDao appOrg = asyncApplicantOrg.get();

        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");

        // Get applicant's org manager
        orgMan = appOrg.getManager();

        if (!data.getProperties().containsKey("ownerId")) throw new MissingAttributeException("ownerId");
        CompletableFuture<OrganizationDao> asyncOwnerOrg = service.getOrganization(data.getProperties().get("ownerId"));
        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(data.getProperties().get("assetId"));

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

                        buildNotification(notification, orgMan, meta, content, "CONTRACT_OFFER_ACCEPTED", "Contract Offer Accepted");
                        break;
                    }
        }

        System.out.println("Processing 'contract offer accepted' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("assetName", assetName);
        meta.put("targetType", data.getProperties().get("targetType"));
        meta.put("targetId", data.getProperties().get("assetId"));
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

    public UserDao getOrgMan() {
        return orgMan;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getOrgName() {
        return orgName;
    }
}
