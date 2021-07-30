package aero.icarus2020.models;

import java.util.Date;
import java.util.HashMap;

public class NotificationEvent {
    private String eventType;
    private HashMap<String, String> properties = new HashMap<String, String>();
    private Date timestamp;

    public NotificationEvent() {
        this.timestamp = new java.util.Date();
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public String getEventType() {
        return eventType;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        String out = "{eventType:" + this.eventType;
        out += ",properties:{";
        for (String i : this.properties.keySet()) {
            out += i + ":" + this.properties.get(i);
        }
        out += "},timestamp:" + this.timestamp + "}";
        return out;
    }
}
