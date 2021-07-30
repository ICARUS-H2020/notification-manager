package aero.icarus2020.models;

import java.util.ArrayList;

public class UserDao {

    private long id;
    private long organizationid;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private boolean firstlogin;
    private boolean enabled;
    private boolean passwordexpired;
    private String position;
    private String department;
    private String phone;
    private String image;
    private ArrayList<String> userRoles;

    public UserDao() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrganizationid() {
        return organizationid;
    }

    public void setOrganizationid(long organizationid) {
        this.organizationid = organizationid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public boolean isFirstlogin() {
        return firstlogin;
    }

    public void setFirstlogin(boolean firstlogin) {
        this.firstlogin = firstlogin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPasswordexpired() {
        return passwordexpired;
    }

    public void setPasswordexpired(boolean passwordexpired) {
        this.passwordexpired = passwordexpired;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public ArrayList<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(ArrayList<String> userRoles) {
        this.userRoles = userRoles;
    }

    @Override
    public String toString() {
        String str = "\nID: " + this.id;
        str += "\nOrganization ID: " + this.organizationid;
        str += "\nUsername: " + this.username;
        str += "\nEmail: " + this.email;
        str += "\nFirstname: " + this.firstname;
        str += "\nLastname: " + this.lastname;
        str += "\nFirst Login: " + this.firstlogin;
        str += "\nEnabled: " + this.enabled;
        str += "\nPassword Expired: " + this.passwordexpired;
        str += "\nPosition: " + this.position;
        str += "\nDepartment: " + this.department;
        str += "\nPhone: " + this.phone;
        str += "\nImage: " + this.image;
        str += "\nUser Roles: " + this.userRoles.toString();
        return str;
    }
}
