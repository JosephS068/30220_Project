package client;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.Console;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import core.MessageInfo;
import core.ChannelInfo;

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

        getUsername();
        joinChannel();

        MessageInfo[] loginMessages = rest.getForObject(currentChannel.address + "/loginMessages", MessageInfo[].class);

        // Prints past messages and saves latest spot
        for (MessageInfo messageData : loginMessages) {
            // only print messages from other users
            System.out.println(messageData.username + "> " + messageData.message);
            currentMessageId = messageData.sequenceId;
        }

        // Actively checks for new messages and update the UI if new messages are found
        MessageUpdater updater = new MessageUpdater();
        updater.start();
        while(runClient) {
            String message = console.readLine();
            if(message.charAt(0) == '/') {
                clientCommand(message);
            } else if (message.charAt(0) == '!') {
                botCommand(message);
            } else {
                sendMessage(message);
            }
        }

        // Client has ended, close scanner and end thread
        System.out.println("Shutting down client");
        updater.stop();
    }

    public static void getUsername() {
        boolean validUsername = false;
        do {
            System.out.println("Please enter in a username");
            username = console.readLine();
            // [] are for bot names only
            if (username.contains("[") || username.contains("]")) {
                System.out.println("You cannot have \'[\' or \']\' in your username, these characters are reserved for bots");
            } else if (username.length() < 4) {
                System.out.println("Your username must have at least 4 letters");
            } else {
                validUsername = true;
            }
        } while (!validUsername);
    }

    public static String displayWelcomeMessage() {
        String welcomeMessage = "Welcome to: " + currentChannel.name + "\n"
        + "@ " + currentChannel.address + "\n"
        + "Description-----------------" + "\n"
        + currentChannel.description + "\n"
        + "----------------------------";
        return welcomeMessage;
    }
    
    public static void clientCommand(String message) {
        String command = message.substring(1);
        String commandParts[] = command.split(" ", 2);
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
    }

    public static void addBot(String[] commandParts) {
        if (commandParts.length == 2) {
            rest.put(currentChannel.address + "/bot/" + commandParts[1], null);
        } else {
            System.out.println("Incorrect number of arguements have been past to add bot");
        }
    }
    
    public static void removeBot(String[] commandParts) {
        if (commandParts.length == 2) {
            rest.delete(currentChannel.address + "/bot/" + commandParts[1]);
        } else {
            System.out.println("Incorrect number of arguements have been past to remove bot");
        }
    }
    
    public static void showBots() {
        String bots = rest.getForObject(currentChannel.address + "/bots", String.class);
        System.out.print(bots);
    }

    public static void joinChannel() {
        String channelList = rest.getForObject("http://localhost:8080/channels", String.class);
        System.out.println("");
        System.out.println("Please enter the channel name you would like to join");
        System.out.println("----------------------------------------------------");
        System.out.print(channelList);
        System.out.println("----------------------------------------------------");
        String channel = console.readLine();

        try {
            currentChannel = rest.getForObject("http://localhost:8080/channel/" + channel, ChannelInfo.class);
            currentMessageId = -1;
            displayWelcomeMessage();
        } catch (HttpClientErrorException e) {
            System.out.println("Couldn not join specified channel, please try again");
            // have the user join a channel again
            joinChannel();
        }
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

// Thread which prints result from broker
class MessageUpdater implements Runnable {
    private Thread thread;
    private boolean exit=false;

    public MessageUpdater() {
    }

	public void run() {
        try {
            RestTemplate rest = new RestTemplate();
            while(!exit) {
                MessageInfo[] messages = rest.getForObject(Client.currentChannel.address + "/getMessages/" + Client.currentMessageId, MessageInfo[].class);
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