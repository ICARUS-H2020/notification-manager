package aero.icarus2020.models;

import javax.management.Notification;
import java.util.ArrayList;
import java.util.List;

public class OrganizationDao {

    private long id;
    private String legalname;
    private String businessname;
    private String description;
    //    private Object wallet;
    private String address;
    private String websiteurl;
    private String city;
    private String country;
    private String postalcode;
    private Object logoimage;
    private Object bannerimage;
    private Object ethaddress;
    private Object ethwallet;
    private Object type;
    private UserDao manager;
    private ArrayList<CategoriesDao> categories;
    private PreferencesDao preferences;

    public OrganizationDao() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLegalname() {
        return legalname;
    }

    public void setLegalname(String legalname) {
        this.legalname = legalname;
    }

    public String getBusinessname() {
        return businessname;
    }

    public void setBusinessname(String businessname) {
        this.businessname = businessname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsiteurl() {
        return websiteurl;
    }

    public void setWebsiteurl(String websiteurl) {
        this.websiteurl = websiteurl;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public Object getLogoimage() {
        return logoimage;
    }

    public void setLogoimage(Object logoimage) {
        this.logoimage = logoimage;
    }

    public Object getBannerimage() {
        return bannerimage;
    }

    public void setBannerimage(Object bannerimage) {
        this.bannerimage = bannerimage;
    }

    public Object getEthaddress() {
        return ethaddress;
    }

    public void setEthaddress(Object ethaddress) {
        this.ethaddress = ethaddress;
    }

    public Object getEthwallet() {
        return ethwallet;
    }

    public void setEthwallet(Object ethwallet) {
        this.ethwallet = ethwallet;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public UserDao getManager() {
        return manager;
    }

    public void setManager(UserDao manager) {
        this.manager = manager;
    }

    public ArrayList<CategoriesDao> getCategories() {
        return this.categories;
    }

    public void setCategories(ArrayList<CategoriesDao> categories) {
        this.categories = categories;
    }

    public PreferencesDao getPreferences() { return this.preferences; }

    public void setPreferences(PreferencesDao preferences) { this.preferences = preferences; }

    public ArrayList<NotificationsDao> getNotifications() { return this.preferences.getNotifications(); }

    public void setNotifications(ArrayList<NotificationsDao> notifications) { this.preferences.setNotifications(notifications); }
}
