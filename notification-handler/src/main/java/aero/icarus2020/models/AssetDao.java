package aero.icarus2020.models;

import java.util.ArrayList;

public class AssetDao {
    private long id;
    private long datajobid;
    private long downloads;
    private String name;
    private String description;
    private long coverphoto;
    private String status;
    private String created;
    private String updated;

    private OrganizationShortDao organization;
    private Object metadata;
    private Object sample;
    private Object user;
    private ArrayList<CategoriesDao> categories;

    public AssetDao() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDatajobid() {
        return datajobid;
    }

    public void setDatajobid(long datajobid) {
        this.datajobid = datajobid;
    }

    public long getDownloads() {
        return downloads;
    }

    public void setDownloads(long downloads) {
        this.downloads = downloads;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCoverphoto() {
        return coverphoto;
    }

    public void setCoverphoto(long coverphoto) {
        this.coverphoto = coverphoto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public OrganizationShortDao getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationShortDao organization) {
        this.organization = organization;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public Object getSample() {
        return sample;
    }

    public void setSample(Object sample) {
        this.sample = sample;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public ArrayList<CategoriesDao> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<CategoriesDao> categories) {
        this.categories = categories;
    }
}
