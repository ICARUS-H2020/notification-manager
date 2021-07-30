package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DatasetDownloadFailedManager extends Manager {

    private String datasetName;
    private String datasetId;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'dataset download failed job' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("recipientId")) throw new MissingAttributeException("recipientId");
        CompletableFuture<UserDao> asyncRecipient = service.getUser(data.getProperties().get("recipientId"));
        UserDao recipient = asyncRecipient.get();

        CompletableFuture<OrganizationDao> asyncOrganization = service.getOrganization(String.valueOf(recipient.getOrganizationid()));
        OrganizationDao orgRecipient = asyncOrganization.get();

        if (!data.getProperties().containsKey("datasetId")) throw new MissingAttributeException("datasetId");
        if (!data.getProperties().containsKey("datasetName")) throw new MissingAttributeException("datasetName");
        datasetId = data.getProperties().get("datasetId");
        datasetName = data.getProperties().get("datasetName");

        if (orgRecipient.getPreferences() != null) {
            ArrayList<NotificationsDao> notifications = orgRecipient.getNotifications();

            // check if the organization wants to get this kind of notifications
            if (notifications != null)
                for (NotificationsDao notification : notifications)
                    if (notification.getLabel().compareToIgnoreCase("datasets") == 0) {

                        // Build meta info
                        HashMap<String, String> meta = buildMeta();

                        // Build email content
                        HashMap<String, String> content = buildContent();

                        buildNotification(notification, recipient, meta, content, "DATASET_DOWNLOAD_FAILED", "Dataset Download Failed");

                        return;
                    }
        }

        System.out.println("Processing 'dataset download failed job' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("assetName", datasetName);
        meta.put("assetId", datasetId);

        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("assetName", datasetName);

        return content;
    }
}