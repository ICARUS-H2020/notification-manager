package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DataCheckinJobCompletedManager extends Manager {
    private String orgName;
    private String assetName;
    private String targetType;
    private long assetId;
    private AssetDao asset;
    private String visibility;
    private long orgId;

    /**
     * This method is responsible to check if the organization-owner of the dataset has a preference to get a
     * notification and call sendNotificationToOwnerOrganization and sendEmail. It also checks if
     * the dataset if public/private, if it is public/private it will call sendNotificationToOtherOrganizations.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {

        System.out.println("Processing 'dataset added' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("assetId")) throw new MissingAttributeException("assetId");
        CompletableFuture<AssetDao> asyncAsset = service.getAsset(data.getProperties().get("assetId"));

        // Get asset
        asset = asyncAsset.get();

        assetName = asset.getName();
        assetId = asset.getId();

        orgName = asset.getOrganization().getName();

        if (!data.getProperties().containsKey("visibility")) throw new MissingAttributeException("visibility");
        visibility = data.getProperties().get("visibility");

        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");

        orgId = asset.getOrganization().getId();

        CompletableFuture<OrganizationDao> asyncAppOrg = service.getOrganization(String.valueOf(orgId));
        OrganizationDao appOrg = asyncAppOrg.get();

        if (appOrg.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = appOrg.getNotifications();

            // check if the organization wants to get this kind of notifications
            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("data-check-in") == 0) {
                        // find user owner's email

                        if (!data.getProperties().containsKey("userOwnerId"))
                            throw new MissingAttributeException("userOwnerId");
                        CompletableFuture<UserDao> asyncUser = service.getUser(data.getProperties().get("userOwnerId"));
                        UserDao ownerUser = asyncUser.get();

                        // Build email content
                        HashMap<String, String> content = buildContent();
                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        buildNotification(notification, ownerUser, meta, content, "DATA_CHECKIN_JOB_COMPLETED", "Data Check-in Job Completed");
                        break;
                    }
        }

        sendNotificationToOtherOrganizations();

        System.out.println("Processing 'dataset added' event...done");

    }

    /**
     * Sends notifications to all members of other organizations that have as a preference to get a notification when a
     * new dataset is added
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void sendNotificationToOtherOrganizations() throws InterruptedException, ExecutionException {

        // get all asset's categories
        List<CategoriesDao> assetCategories = asset.getCategories();
        List<String> orgCategories;

        // get all organizations
        CompletableFuture<Long[]> asyncOrgs = service.getAllAuthorisecOrganizations(String.valueOf(assetId));
        Long[] authorizedOrgs = asyncOrgs.get();

        boolean flag = false, flag_notifications = false, flag_emails = false;
        String assetCategory = "", target = "", email = "";
        long recipientId = 0;

        // check for each organization if they are interested in this dataset's category
        for (int i = 0; i < authorizedOrgs.length; i++) {

            CompletableFuture<OrganizationDao> asyncOrg = service.getOrganization(String.valueOf(authorizedOrgs[i]));
            OrganizationDao appOrg = asyncOrg.get();

            if (appOrg.getPreferences() != null && appOrg.getId() != orgId) {
                ArrayList<NotificationsDao> notifications = appOrg.getNotifications();
                NotificationsDao targetNotification = new NotificationsDao();

                flag_notifications = false;
                flag_emails = false;
                target = "";
                if (notifications != null)
                    for (NotificationsDao notification : notifications)
                        if (notification.getLabel().compareToIgnoreCase("datasets") == 0) {
                            flag_notifications = notification.getNotifications();
                            flag_emails = notification.getEmail();
                            target = notification.getTarget();
                            targetNotification = notification;
                            break;
                        }

                if (flag_notifications || flag_emails) {
                    orgCategories = targetNotification.getCategories();
                    flag = false;
                    for (String orgCategory : orgCategories) {
                        for (CategoriesDao assetCategory_i : assetCategories)
                            if (assetCategory_i.getId().compareToIgnoreCase(orgCategory) == 0) {
                                flag = true;
                                assetCategory = assetCategory_i.getName();
                                break;
                            }
                        if (flag) break;
                    }
                }

                // if organization has a preference to get a notification / email
                if (flag) {
                    if (target != null && (target.equalsIgnoreCase("2") || target.equalsIgnoreCase("1"))) {

                        // get all organization's members
                        CompletableFuture<UserDao[]> asyncMembers = service.getOrganizationUsers(String.valueOf(appOrg.getId()));
                        UserDao[] orgMembers = asyncMembers.get();

                        for (int j = 0; j < orgMembers.length; j++) {
                            if ((target.equalsIgnoreCase("1") && orgMembers[j].getUserRoles().contains("ORGANISATIONUSER")) || target.equalsIgnoreCase("2")) {
                                recipientId = orgMembers[j].getId();
                                email = orgMembers[j].getEmail();

                                if (flag_notifications)
                                    webNotificationsList.add(sendNotificationToMember(assetCategory, recipientId));
                                if (flag_emails)
                                    emailNotificationsList.add(sendEmailToMember(email));
                            }
                        }
                    } else {
                        recipientId = appOrg.getManager().getId();
                        email = appOrg.getManager().getEmail();
                        if (flag_notifications)
                            webNotificationsList.add(sendNotificationToMember(assetCategory, recipientId));
                        if (flag_emails)
                            emailNotificationsList.add(sendEmailToMember(email));
                    }
                }
            }
        }
    }

    /**
     * Create and send notification to the organization member
     *
     * @param assetCategory, asset's category
     * @param recipientId,   recipient's id
     */
    private WebNotification sendNotificationToMember(String assetCategory, long recipientId) {

        /* variables that will be used */
        // meta data of the notification
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("assetName", assetName);
        meta.put("assetCategory", assetCategory);
        meta.put("targetType", targetType);
        meta.put("targetId", String.valueOf(assetId));
        meta.put("visibility", visibility);

        // Create a notification record
        WebNotification wn = new WebNotification();
        wn.setNotif_type("DATASET_ADDED");
        wn.setRecipient_id(recipientId);
        wn.setMeta(meta);

        // Store notification
        WebNotification storedWN = webNotificationRepository.save(wn);

        // Send real time notification (web-sockets)
        websocketSender.send(storedWN);

        return wn;
    }

    /**
     * Create and send an email Notification
     *
     * @param email, recipient's email
     */
    private EmailNotification sendEmailToMember(String email) {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        content.put("orgName", orgName);

        EmailNotification en = new EmailNotification();
        en.setContent(content);
        en.setTo(email);
        en.setType("DATASET_ADDED");
        en.setSubject("Availability of a New Data Asset");

        emailSender.send(en);

        return en;
    }

    /**
     * By default this method will build the notification's meta for the owner organization
     */
    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("senderName", orgName);
        meta.put("dataCheckinJobName", assetName);
        meta.put("targetType", targetType);
        meta.put("targetId", String.valueOf(assetId));
        meta.put("visibility", visibility);
        return meta;
    }

    /**
     * By default this method will build the email's content for the owner organization
     */
    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", assetName);
        return content;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getTargetType() {
        return targetType;
    }

    public AssetDao getAsset() {
        return asset;
    }

    public long getAssetId() {
        return assetId;
    }

    public String getVisibility() {
        return visibility;
    }

    public long getOrgId() {
        return orgId;
    }
}
