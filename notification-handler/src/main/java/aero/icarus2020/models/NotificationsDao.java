package aero.icarus2020.models;

import java.util.ArrayList;

public class NotificationsDao {

    private String label;
    private String description;
    // Later it will be changed to ArrayList<CategoriesDao>
    private ArrayList<String> categories;
    private boolean notifications;
    private boolean email;
    private String target;

    public boolean isNotifications() {
        return notifications;
    }

    public boolean isEmail() {
        return email;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }

    public boolean getNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public boolean getEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }
}
