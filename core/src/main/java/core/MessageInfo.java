package core;

public class MessageInfo {
    public String username;
    public String message;

    public MessageInfo(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public MessageInfo() {}

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}