package core;

public class BotCommand {
    public String responseAddress;
    public MessageInfo info;

    public BotCommand(String responseAddress, MessageInfo info) {
        this.responseAddress = responseAddress;
        this.info = info;
    }

    public BotCommand() {
    }

    public String getResponseAddress() {
        return this.responseAddress;
    }

    public void setResponseAddress(String responseAddress) {
        this.responseAddress = responseAddress;
    }

    public MessageInfo getInfo() {
        return this.info;
    }

    public void setInfo(MessageInfo info) {
        this.info = info;
    }
}