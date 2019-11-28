package client;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.Console;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;
import core.MessageInfo;

public class Client {
    public static String username;
    public static void main(String[] args) {
        // Stop logging
        Logger.getRootLogger().setLevel(Level.OFF);
        Logger.getLogger(RestTemplate.class.getName()).setLevel(Level.OFF);

        //for reading input
        Scanner input = new Scanner(System.in);

        // TODO Limit username to not have brackets 
        System.out.println("Please enter in a username");
        String username = input.nextLine();

        RestTemplate rest = new RestTemplate();
        MessageInfo[] loginMessages = rest.getForObject("http://localhost:8084/loginMessages", MessageInfo[].class);

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
        boolean runClient = true;
        Console console = System.console();
        while(runClient) {
            String message = console.readLine();
            // Handle user commands
            if(message.equals("!quit")) {
                runClient = false;
            // Handling bot commands
            } else if (message.charAt(0) == '!') {
                String commandParts[] = message.split(" ", 2);
                // Get bot name from first word, remove ! from name
                String botName = commandParts[0].substring(1);
                String parameters = commandParts[1];
                MessageInfo info = new MessageInfo(username, parameters);
                rest.put("http://localhost:8084/commandBot/"+botName, info);
                // TODO maybe make post for errors
            // Regular Message to be sent to server
            } else {
                MessageInfo info = new MessageInfo(username, message);
                rest.put("http://localhost:8084/message", info);
            }
        }

        // Client has ended, close scanner and end thread
        System.out.println("Shutting down client");
        updater.stop();
        input.close();
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
                MessageInfo[] messages = rest.getForObject("http://localhost:8084/getMessages/"+currentMessageId, MessageInfo[].class);
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