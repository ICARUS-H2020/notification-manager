package aero.icarus2020.managers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class SuggestionAcceptedManager extends Manager {

    private String title;
    private String targetType;
    private String targetId;

    /**
     * Check in organization's preferences they want to get a notification or/and email, then send them.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void sendNotification() throws InterruptedException, ExecutionException, MissingAttributeException {
        System.out.println("Processing 'suggestion accepted' event...");

        wn = null;
        en = null;
        webNotificationsList = new ArrayList<>();
        emailNotificationsList = new ArrayList<>();

        if (!data.getProperties().containsKey("recipientId")) throw new MissingAttributeException("recipientId");
        CompletableFuture<OrganizationDao> asyncOrganization = service.getOrganization(data.getProperties().get("recipientId"));

        // Get org name
        OrganizationDao orgRecipient = asyncOrganization.get();

        if (!data.getProperties().containsKey("targetType")) throw new MissingAttributeException("targetType");
        targetType = data.getProperties().get("targetType");
        if (!data.getProperties().containsKey("targetId")) throw new MissingAttributeException("targetId");
        targetId = data.getProperties().get("targetId");
        if (!data.getProperties().containsKey("conceptTitle")) throw new MissingAttributeException("conceptTitle");
        title = data.getProperties().get("conceptTitle");

        UserDao recipientId = orgRecipient.getManager();

        // mock preferences, since by default the user will be notified for their suggestion
        NotificationsDao notification = new NotificationsDao();
        notification.setNotifications(true);
        notification.setEmail(true);

        // Build email content
        HashMap<String, String> content = buildContent();
        // Build meta info
        HashMap<String, String> meta = buildMeta();

        buildNotification(notification, recipientId, meta, content, "SUGGESTION_ACCEPTED", "Concept Suggestion Accepted");

        System.out.println("Processing 'suggestion accepted' event...done");
    }

    @Override
    HashMap<String, String> buildMeta() {
        // Build meta info
        HashMap<String, String> meta = new HashMap<String, String>();
        meta.put("title", title);
        meta.put("targetType", targetType);
        meta.put("targetId", targetId);
        return meta;
    }

    @Override
    HashMap<String, String> buildContent() {
        // Build email content
        HashMap<String, String> content = new HashMap<String, String>();
        content.put("title", title);
        return content;
    }
}
