package core;

public class MessageInfo {
    public String username;
    public String message;
    public int sequenceId;

    public MessageInfo(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public MessageInfo(String username, String message, int sequenceId) {
        this.username = username;
        this.message = message;
        this.sequenceId = sequenceId;
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

    public int getSequenceId() {
        return this.sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }
}