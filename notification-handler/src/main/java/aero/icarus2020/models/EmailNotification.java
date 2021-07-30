package aero.icarus2020.models;

import java.util.Date;
import java.util.HashMap;

public class EmailNotification {

    private String type;
    private String to;
    private String subject;
    private HashMap<String, String> content = new HashMap<String, String>();

    public EmailNotification() { }

    public EmailNotification(String type, String to, String subject, HashMap<String, String> content) {
        this.type = type;
        this.to = to;
        this.subject = subject;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public HashMap<String, String> getContent() {
        return content;
    }

    public void setContent(HashMap<String, String> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        String out = "\nType: " + this.type;
        out += "\nSend To: " + this.to;
        out += "\nSubject: " + this.subject;
        out += "\nContent: [";
        for (String i : this.content.keySet()) {
            out += "\n\t" + i + ": " + this.content.get(i);
        }
        out += "\n]";
        return out;
    }
}
