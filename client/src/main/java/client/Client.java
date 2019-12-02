package client;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.Console;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import core.MessageInfo;
import core.ChannelCommand;
import core.ChannelInfo;
import core.CommandLineHelper;

public class Client {
    public static String username;
    public static ChannelInfo currentChannel;
    public static int currentMessageId;

    public static boolean runClient = true;

    public static RestTemplate rest = new RestTemplate();
    public static Console console = System.console();
    
    public static void main(String[] args) {
        // Stop logging
        Logger.getRootLogger().setLevel(Level.OFF);
        Logger.getLogger(RestTemplate.class.getName()).setLevel(Level.OFF);

        username = CommandLineHelper.getUsername();
        joinChannel();

        // Actively checks for new messages and update the UI if new messages are found
        MessageUpdater updater = new MessageUpdater();
        updater.start();

        while(runClient) {
            String message = CommandLineHelper.noNullInput();
            if (message != null) {
                if (message.charAt(0) == '/') {
                    clientCommand(message);
                } else if (message.charAt(0) == '!') {
                    botCommand(message);
                } else {
                    sendMessage(message);
                }
            }
        }

        // Client has ended, close scanner and end thread
        System.out.println("Shutting down client");
        updater.stop();
    }

    public static void joinChannel() {
        String channelList = rest.getForObject("http://localhost:8080/channels", String.class);
        System.out.println("");
        System.out.println("Please enter the channel name you would like to join");
        System.out.println("----------------------------------------------------");
        System.out.print(channelList);
        System.out.println("----------------------------------------------------");
        String channel = CommandLineHelper.noNullInput();
        try {
            logIntoChannel(channel);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            switch (statusCode) {
                case 403:
                    System.out.println("You are banned, join a different channel or get out");
                    joinChannel();
                    break;
                case 404:
                    System.out.println("Channel could not be found, please try again");
                    joinChannel();
                    break;
            }
        }
    }

    public static void logIntoChannel(String channel) {
        currentChannel = rest.getForObject("http://localhost:8080/channel/" + channel, ChannelInfo.class);
        currentMessageId = -1;
        Boolean requiresPin = rest
                .getForObject(currentChannel.address + "/authenticate/" + username, Boolean.class)
                .booleanValue();

        boolean gainedAccess = true;
        if (requiresPin.booleanValue()) {
            gainedAccess = providePin(); 
        }

        if (gainedAccess) { 
            displayWelcomeMessage();
            displayLoginMessages();
        } else {
            System.out.println("You did not gain access to channel, please join another one");
            joinChannel();
        }
    }

    public static boolean providePin() {
        System.out.println("You must provide a pin to server");
        boolean validPin;
        // TODO test this one for null
        char[] pin = console.readPassword();
        try {
            rest.put(currentChannel.address + "/authenticate/" + username, pin);
            validPin = true;
            return validPin;
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            validPin = false;
            switch (statusCode) {
            case 401:
                System.out.println("You entered in the wrong pin would you like to try again?(y/n)");
                boolean enterPinAgain = CommandLineHelper.responseYesNo();
                if (enterPinAgain) {
                    validPin = providePin();
                }
                break;
            default:
                break;
            }
            return validPin;
        }
    }

    public static void displayWelcomeMessage() {
        String welcomeMessage = "Welcome to: " + currentChannel.name + "\n"
        + "@ " + currentChannel.address + "\n"
        + "Description-----------------" + "\n"
        + currentChannel.description + "\n"
        + "----------------------------";
        System.out.println(welcomeMessage);
    }

    public static void displayLoginMessages() {
        MessageInfo[] loginMessages = rest.getForObject(currentChannel.address + "/message", MessageInfo[].class);
        // Prints past messages and saves latest spot
        for (MessageInfo messageData : loginMessages) {
            // only print messages from other users
            System.out.println(messageData.username + "> " + messageData.message);
            currentMessageId = messageData.sequenceId;
        }
    }
    
    public static void clientCommand(String message) {
        String command = message.substring(1);
        String commandParts[] = command.split(" ", 2);
        try {
            switch (commandParts[0]) {
            case "add_bot":
                addBot(commandParts);
                break;
            case "remove_bot":
                removeBot(commandParts);
                break;
            case "show_bots":
                showBots();
                break;
            case "ban":
                banUser(commandParts);
                break;
            case "unban":
                unbanUser(commandParts);
                break;
            case "show_bans":
                showBans();
                break;
            case "add_admin":
                addAdmin(commandParts);
                break;
            case "remove_admin":
                removeAdmin(commandParts);
                break;
            case "show_admins":
                showAdmins();
                break;
            case "join":
                joinChannel();
                break;
            case "quit":
                runClient = false;
                break;
            default:
                System.out.println("Unknown client command");
                break;
            }
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            switch (statusCode) {
            case 401:
                System.out.println("You can't use this command you are not an admin");
                break;
            }
        }
    }

    public static void addBot(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.put(currentChannel.address + "/bot", command);
        }
    }
    
    public static void removeBot(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.delete(currentChannel.address + "/bot/" + command.issuer + "/" + command.subject);
        }
    }
    
    public static void showBots() {
        String bots = rest.getForObject(currentChannel.address + "/bots", String.class);
        System.out.print(bots);
    }

    public static void banUser(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.put(currentChannel.address + "/ban", command);
        }
    }

    public static void unbanUser(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.delete(currentChannel.address + "/ban/" + command.issuer + "/" + command.subject);
        }
    }

    public static void showBans() {
        String bans = rest.getForObject(currentChannel.address + "/ban", String.class);
        System.out.print(bans);
    }

    public static void addAdmin(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.put(currentChannel.address + "/admin", command);
        } 
    }

    public static void removeAdmin(String[] commandParts) {
        if (validArguements(2, commandParts)) {
            ChannelCommand command = new ChannelCommand(username, commandParts[1]);
            rest.delete(currentChannel.address + "/admin/" + command.issuer + "/" + command.subject);
        }
    }

    public static void showAdmins() {
        String bans = rest.getForObject(currentChannel.address + "/admin", String.class);
        System.out.print(bans);
    }

    public static boolean validArguements(int numArgs, String[] args) {
        if (numArgs != args.length) {
            System.out.println("Incorrect number of arguements past to command");
            return false;
        }
        return true;
    }

    public static void botCommand(String message) {
        String commandParts[] = message.split(" ", 2);
        // Get bot name from first word, remove ! from name
        String botName = commandParts[0].substring(1);

        String parameters = "";
        if (commandParts.length == 2) {
            parameters = commandParts[1];
        }

        MessageInfo info = new MessageInfo(username, parameters);
        try {
            rest.put(currentChannel.address + "/commandBot/" + botName, info);
        } catch (HttpClientErrorException e) {
            System.out.println("Bot called does not exist");
        } catch (HttpServerErrorException e) {
            System.out.println("Bot was not given proper parameters");
        }
    }

    public static void sendMessage(String message) {
        MessageInfo info = new MessageInfo(username, message);
        rest.put(currentChannel.address + "/message", info);
    }
}

class MessageUpdater implements Runnable {
    private Thread thread;
    private boolean exit=false;

    public MessageUpdater() {}

	public void run() {
        try {
            RestTemplate rest = new RestTemplate();
            while(!exit) {
                MessageInfo[] messages = rest.getForObject(Client.currentChannel.address + "/message/" + Client.currentMessageId, MessageInfo[].class);
                for (MessageInfo messageData : messages) {
                    // only print messages from other users
                    System.out.println(messageData.username + "> " + messageData.message);
                    Client.currentMessageId = messageData.sequenceId;
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            System.out.println("Error occured while reading new messages");
        }
    }
    
    public void stop() {
        exit = true;
    }

	public void start() {
        if (thread == null) {
            thread = new Thread(this, "Get messages");
            thread.start();
        }
	}
}