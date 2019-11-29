package client;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.Console;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;
import core.MessageInfo;
import core.ChannelInfo;

public class Client {
    public static String username;
    public static ChannelInfo currentChannel;

    public static boolean runClient = true;

    public static RestTemplate rest = new RestTemplate();
    public static Console console = System.console();
    
    public static void main(String[] args) {
        // Stop logging
        Logger.getRootLogger().setLevel(Level.OFF);
        Logger.getLogger(RestTemplate.class.getName()).setLevel(Level.OFF);

        // TODO Limit username to not have brackets 
        System.out.println("Please enter in a username");
        username = console.readLine();

        joinChannel();

        MessageInfo[] loginMessages = rest.getForObject(currentChannel.address + "/loginMessages", MessageInfo[].class);

        // Prints past messages and saves latest spot
        int currentMessageId = 0;
        for (MessageInfo messageData : loginMessages) {
            // only print messages from other users
            System.out.println(messageData.username + "> " + messageData.message);
            currentMessageId = messageData.sequenceId;
        }

        // Actively checks for new messages and update the UI if new messages are found
        MessageUpdater updater = new MessageUpdater(currentMessageId);
        updater.start();
        while(runClient) {
            String message = console.readLine();
            if(message.charAt(0) == '!') {
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

    public static void clientCommand(String message) {
        String command = message.substring(1);
        switch (command) {
        case "quit":
            runClient = false;
            break;
        case "join":
            joinChannel();
            break;
        default:
            System.out.println("Unknown client command");
            break;
        }
    }

    public static void joinChannel() {
        // selecting channel
        String channelList = rest.getForObject("http://localhost:8080/channels", String.class);
        System.out.println("");
        System.out.println("Please enter the channel name you would like to join");
        System.out.println("----------------------------------------------------");
        System.out.println(channelList);
        System.out.println("----------------------------------------------------");
        String channel = console.readLine();
        // Put in try catch for incorrect channel name
        currentChannel = rest.getForObject("http://localhost:8080/channel/info/"+channel, ChannelInfo.class);
    }

    public static void botCommand(String message) {
        String commandParts[] = message.split(" ", 2);
        // Get bot name from first word, remove ! from name
        String botName = commandParts[0].substring(1);
        String parameters = commandParts[1];
        MessageInfo info = new MessageInfo(username, parameters);
        rest.put(currentChannel.address + "/commandBot/" + botName, info);
        // TODO maybe make post for errors
    }

    public static void sendMessage(String message) {
        MessageInfo info = new MessageInfo(username, message);
        rest.put(currentChannel.address + "/message", info);
    }
}

// Thread which prints result from broker
class MessageUpdater implements Runnable {
    private Thread thread;
    private int currentMessageId;
    private boolean exit=false;

    public MessageUpdater(int currentMessageId) {
        this.currentMessageId = currentMessageId;
    }

	public void run() {
        try {
            RestTemplate rest = new RestTemplate();
            while(!exit) {
                // TODO fix issue where we aren't getting the very first message typed
                MessageInfo[] messages = rest.getForObject(Client.currentChannel.address + "/getMessages/"+currentMessageId, MessageInfo[].class);
                for (MessageInfo messageData : messages) {
                    // only print messages from other users
                    System.out.println(messageData.username + "> " + messageData.message);
                    currentMessageId = messageData.sequenceId;
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
            thread = new Thread(this, "Print Results");
            thread.start();
        }
	}
}