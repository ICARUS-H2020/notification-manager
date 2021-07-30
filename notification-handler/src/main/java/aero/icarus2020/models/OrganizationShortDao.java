package aero.icarus2020.models;

public class OrganizationShortDao {
    private long id;
    private String name;
    private String logoimage;

    public OrganizationShortDao() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoimage() {
        return logoimage;
    }

    public void setLogoimage(String logoimage) {
        this.logoimage = logoimage;
    }

    @Override
    public String toString() {
        String str = "\nID: " + this.id;
        str += "\nName: " + this.name;
        str += "\nLogo Image: " + this.logoimage;
        return str;
    }
}
