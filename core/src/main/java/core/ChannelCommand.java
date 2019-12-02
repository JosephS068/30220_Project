package core;

public class ChannelCommand {
    public String issuer;
    public String subject;

    public ChannelCommand(String issuer, String subject) {
        this.issuer = issuer;
        this.subject = subject;
    }

    public ChannelCommand() {
    }

    public String getIssuer() {
        return this.issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}