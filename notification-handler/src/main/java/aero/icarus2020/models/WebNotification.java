package aero.icarus2020.models;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;

@Entity(name = "notifications")
@Table(name = "notifications")
public class WebNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_notif_id")
    private long _notif_id;

    @Column(name = "notif_type")
    private String notif_type;

    @Column(name = "recipient_id")
    private long recipient_id;

    @Column(name = "meta")
    @Convert(converter = HashMapConverter.class)
    private HashMap<String, String> meta;

    @Column(name = "seen_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date seen_on;

    @CreationTimestamp
    @Column(name = "_created_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date _created_on;

    public WebNotification() {}

    public WebNotification(String notif_type, long recipient_id, HashMap<String, String> meta) {
        this.notif_type = notif_type;
        this.recipient_id = recipient_id;
        this.meta = meta;
    }

    public long get_notif_id() {
        return _notif_id;
    }

    public void set_notif_id(long _notif_id) {
        this._notif_id = _notif_id;
    }

    public String getNotif_type() {
        return notif_type;
    }

    public void setNotif_type(String notif_type) {
        this.notif_type = notif_type;
    }

    public long getRecipient_id() {
        return recipient_id;
    }

    public void setRecipient_id(long recipient_id) {
        this.recipient_id = recipient_id;
    }

    public HashMap<String, String> getMeta() {
        return meta;
    }

    public void setMeta(HashMap<String, String> meta) {
        this.meta = meta;
    }

    public Date getSeen_on() {
        return seen_on;
    }

    public void setSeen_on(Date seen_on) {
        this.seen_on = seen_on;
    }

    public Date get_created_on() {
        return _created_on;
    }

    public void set_created_on(Date _created_on) {
        this._created_on = _created_on;
    }

    @Override
    public String toString() {
        String str = "\nNotification ID: " + this._notif_id;
        str += "\nType: " + this.notif_type;
        str += "\nRecipient Id: " + this.recipient_id;
        str += "\nMeta: ";
        for (String i : this.meta.keySet()) {
            str += "\n\t" + i + ": " + this.meta.get(i);
        }
        str += "\nSeen On: " + this.seen_on;
        str += "\nCreated On: " + this._created_on;
        return str;
    }
}
