package core;

public class ServerInfo {
    public String name;
    public String address;
    public String responseAddress;
    public String description;

    public ServerInfo(String responseAddress, MessageInfo info) {
        this.responseAddress = responseAddress;
        this.info = info;
    }

    public ServerInfo() {
    }

    public String getResponseAddress() {
        return this.responseAddress;
    }

    public void setResponseAddress(String responseAddress) {
        this.responseAddress = responseAddress;
    }
}