package core;

public class BotInfo {
    public String name;
    public String address;

    public BotInfo(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public BotInfo() {
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
}