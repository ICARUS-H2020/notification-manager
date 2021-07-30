package aero.icarus2020.managers;

import aero.icarus2020.models.*;
import aero.icarus2020.producers.EmailSender;
import aero.icarus2020.producers.WebsocketsSender;
import aero.icarus2020.repositories.WebNotificationRepository;
import aero.icarus2020.services.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;

/**
 * This is an abstract class for Managers.
 */
public abstract class Manager{

    @Autowired
    AsyncService service;

    @Autowired
    WebNotificationRepository webNotificationRepository;

    @Autowired
    WebsocketsSender websocketSender;

    @Autowired
    EmailSender emailSender;

    NotificationEvent data = null;
    WebNotification wn = null;
    EmailNotification en = null;
    List<WebNotification> webNotificationsList = null;
    List<EmailNotification> emailNotificationsList = null;

    /**
     * Sends to email-module the recipient, type of email, subject and content.
     *
     * @param type,           type of email
     * @param subject,        subject of email
     * @param recipientEmail, recipient of the email
     * @param content,        content of the email
     */
    void sendEmail(String type, String subject, String recipientEmail, HashMap<String, String> content) {
        en = new EmailNotification();
        en.setContent(content);
        en.setTo(recipientEmail);
        en.setType(type);
        en.setSubject(subject);
        emailNotificationsList.add(en);

        emailSender.send(en);
    }

    /**
     * This method builds the notification and calls the method sendEmail if the organization's preferences are set to send an email.
     *
     * @param notification,  organization's notifications preferences
     * @param userRecipient, organization's recipient member
     * @param meta,          meta for the notification
     * @param content,       meta for the email
     * @param eventType,     the type of the event
     * @param emailTitle,    the email's title
     */
    void buildNotification(NotificationsDao notification, UserDao userRecipient, HashMap<String, String> meta, HashMap<String, String> content, String eventType, String emailTitle) {
        if (notification.getNotifications()) {

            Long recipientId = userRecipient.getId();

            // Create a notification record
            wn = new WebNotification();
            wn.setNotif_type(eventType);
            wn.setRecipient_id(recipientId);
            wn.setMeta(meta);
            webNotificationsList.add(wn);

            // Store notification
            WebNotification storedWN = webNotificationRepository.save(wn);

            // Send real time notification (web-sockets)
            websocketSender.send(storedWN);
        }

        if (notification.getEmail()) {
            sendEmail(eventType, emailTitle, userRecipient.getEmail(), content);
        }
    }

    /**
     * This method will build the meta information that the produced notification will contain
     *
     * @return the meta information in a hashmap form
     */
    abstract HashMap<String, String> buildMeta();

    /**
     * This method will build the content of the email that will be produced
     *
     * @return the email's content in a hashmap form
     */
    abstract HashMap<String, String> buildContent();

    public AsyncService getService() {
        return service;
    }

    public WebNotificationRepository getWebNotificationRepository() {
        return webNotificationRepository;
    }

    public WebsocketsSender getWebsocketSender() {
        return websocketSender;
    }

    public EmailSender getEmailSender() {
        return emailSender;
    }

    public NotificationEvent getData() {
        return data;
    }

    public void setData(NotificationEvent data) {
        this.data = data;
    }

    public WebNotification getWn() {
        return wn;
    }

    public EmailNotification getEn() {
        return en;
    }

    public List<WebNotification> getWebNotificationsList() {
        return webNotificationsList;
    }

    public List<EmailNotification> getEmailNotificationsList() {
        return emailNotificationsList;
    }
}
