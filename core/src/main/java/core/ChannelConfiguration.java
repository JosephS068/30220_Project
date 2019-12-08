package core;

public class ChannelConfiguration {
    public String name;
    public String address;
    public String description;
    public boolean requiresPin;
    public String pin;

    public ChannelConfiguration(String name, String address, String description, boolean requiresPin, String pin) {
        this.name = name;
        this.address = address;
        this.description = description;
        this.requiresPin = requiresPin;
        this.pin = pin;
    }

    public ChannelConfiguration() {
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

    public boolean getRequiresPin() {
        return this.requiresPin;
    }

    public void setRequiresPin(boolean requiresPin) {
        this.requiresPin = requiresPin;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}