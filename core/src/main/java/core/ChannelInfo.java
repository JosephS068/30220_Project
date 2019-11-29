package core;

public class ChannelInfo {
    public String name;
    public String address;
    public String description;

    public ChannelInfo(String name, String address, String description) {
        this.name = name;
        this.address = address;
        this.description = description;
    }

    public ChannelInfo() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}