package aero.icarus2020.models;

import java.util.ArrayList;

public class PreferencesDao {
    private ArrayList<NotificationsDao> notifications;

    public void setNotifications(ArrayList<NotificationsDao> notifications) {
        this.notifications = notifications;
    }

    public ArrayList<NotificationsDao> getNotifications() {
        return this.notifications;
    }
}
