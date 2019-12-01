package channel;
import com.google.gson.Gson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.Console;

import javax.annotation.PostConstruct;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import core.BotCommand;
import core.BotInfo;
import core.ChannelInfo;
import core.MessageInfo;

@RestController
public class Channel {
    private final static int LOGIN_MESSAGE_AMOUNT = 20;
    private static int currentId = 0;

    private static String name;
    private static String displayName;
    private static String address;
    private static String responseAddress;
    private static String description;
    private static boolean requiresPin;
    private static char[] pin;
    public static ChannelInfo info;

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    private static RestTemplate rest = new RestTemplate();
    public static Console console = System.console();

    private final static Map<String, String> botURLs = new HashMap<String, String>();
    // Create mongodb for this later
    private final static ArrayList<String> authorizedUsers = new ArrayList<String>();
    private final static ArrayList<String> bannedUsers = new ArrayList<String>();
    // private final static ArrayList<String> adminUsers = new ArrayList<String>();
    
    @PostConstruct
    public void init() {
        getServerInformation();

        // Login to MongoDB
        mongo = new MongoClient("localhost", 27017);
        credential = MongoCredential.createCredential("admin", "Channel-Data", "admin".toCharArray());
        // Access database and get collection for message information
        database = mongo.getDatabase("Channel-Data");
        collection = database.getCollection("Message-Information");

        // Send authentication server channel information
        info = new ChannelInfo(name, address, description);
        rest.put("http://localhost:8080/channel", info);

        // Just testing resets collection on start up
        collection.drop();
        database.createCollection("Message-Information");

        AuthServerChecker checker = new AuthServerChecker(name);
        checker.start();
    }

    public void getServerInformation() {
        // check mongo db first
        // Configuration of server from user input
        System.out.println("This server has not been configured before, please provide additional details");
        System.out.println("What is the name of this server?");
        name = console.readLine();
        displayName = "<" + name + ">";

        System.out.println("What is the address of this server(Name:Port)?");
        address = "http://" + console.readLine();
        // TODO take this line out its for testing only
        address = "http://localhost:8084";
        responseAddress = address + "/message";

        System.out.println("What is the description of this server?");
        description = console.readLine();

        addBots();
        boolean validResponse;
        do {
            System.out.println("Does this server need a pin?(y/n)");
            String response = console.readLine();
            if (response.equals("y")) {
                validResponse = true;
                requiresPin = true;
                addPin();
            } else if(response.equals("n")){
                validResponse = true;
                requiresPin = false;
            } else {
                validResponse = false;
                System.out.println("please enter in \'y\' or \'n\'");
            }
        } while(!validResponse);
        System.out.println("Thank you, the server will now finish start up");
    }

    public void addBots() {
        BotInfo[] botsInformation = rest.getForObject("http://localhost:8080/botsInformation", BotInfo[].class);
        if (botsInformation.length == 0) {
            System.out.println("No bots found skipping this section of configuration");
            return;
        }

        System.out.println("Which bots would you like to be available for this server?");
        System.out.println("type all for all");
        System.out.println("type none for none");
        System.out.println("or list the names seperated by spaces");

        String botList = rest.getForObject("http://localhost:8080/botsList", String.class);
        System.out.println("");
        System.out.println("Currently available bots");
        System.out.println("----------------------------------------------------");
        System.out.print(botList);
        System.out.println("----------------------------------------------------");
        String bots = console.readLine();
        ArrayList<String> botNames = new ArrayList<String>(Arrays.asList(bots.split(" ")));
        
        
        if (bots.toLowerCase().equals("all")) {
            for (BotInfo info : botsInformation) {
                botURLs.put(info.name, info.address);
            }
        } else if (!bots.toLowerCase().equals("none")) {
            for (BotInfo info : botsInformation) {
                if (botNames.contains(info.name)) {
                    botURLs.put(info.name, info.address);
                    botNames.remove(info.name);
                }
            }
            
            if (botNames.size() != 0) {
                System.out.println("The following bots could not be added");
                System.out.println("--------------------------------");
                for (String botName : botNames) {
                    // all the found bots had their names removed, remaining were not found
                    System.out.println(botName);
                }
                System.out.println("--------------------------------");
            }
        }

        System.out.println("The following bots will be added");
        System.out.println("--------------------------------");
        for (String name : botURLs.keySet()) {
            System.out.println(name);
        }
        System.out.println("--------------------------------");
        System.out.println("");
        System.out.println("Does this look correct?(y/n)");
        String response = console.readLine();
        if (!response.toLowerCase().equals("y")) {
            botURLs.clear();
            addBots();
        }
    }

    public void addPin() {
        System.out.println("Please enter in your pin");
        char[] initialPin = console.readPassword();
        System.out.println("Please confirm your pin");
        char[] confirmPin = console.readPassword();
        boolean pinsMatch = true;
        if (initialPin.length == confirmPin.length) {
            for (int i=0; i< initialPin.length; i++) {
                pinsMatch &= initialPin[i] == confirmPin[i];
            }
        } else {
            pinsMatch = false;
        }

        if (!pinsMatch) {
            System.out.println("Your pins do not match, try again");
            addPin();
        } else {
            pin = initialPin;
            System.out.println("Pin for server has been set");
        }
    }

    public void isValidUser(String username) {
        if (requiresPin && !authorizedUsers.contains(username)) {
            throw new NotAuthorizedException();
        } 
        if (bannedUsers.contains(username)) {
            throw new BannedUserException();
        }
    }

    @RequestMapping(value = "/authenticate/{user}", method = RequestMethod.GET)
    public Boolean requiresPin(@PathVariable("user") String username) {
        isValidUser(username);
        return new Boolean(requiresPin);
    }

    @RequestMapping(value = "/authenticate/{user}", method = RequestMethod.PUT)
    public void authenticateUser(@PathVariable("user") String username, @RequestBody char[] enteredPin) {
        boolean pinsMatch = true;
        if (pin.length == enteredPin.length) {
            for (int i = 0; i < enteredPin.length; i++) {
                pinsMatch = pinsMatch && pin[i] == enteredPin[i];
            }
        } else {
            pinsMatch = false;
        }
        
        if (pinsMatch) {
            authorizedUsers.add(username);
        } else {
            throw new InvalidPinException();
        }
    }

    @RequestMapping(value = "/ban", method = RequestMethod.PUT)
    public void banUser(@RequestBody String username) {
        bannedUsers.add(username);
        MessageInfo info = new MessageInfo(displayName, username + " just got clapped");
        sendMessage(info);
        // throw error if you aren't admin
        // thorw error if already present
    }

    @RequestMapping(value = "/ban", method = RequestMethod.DELETE)
    public void unbanUser(@RequestBody String username) {
        bannedUsers.remove(username);
        MessageInfo info = new MessageInfo(displayName, username + "Has risen from the dead, aka unbanned");
        sendMessage(info);
        // throw error if you aren't admin
        // error if not found
    }

    @RequestMapping(value = "/ban", method = RequestMethod.GET)
    public String showBans() {
        String bannedUsernames = "------Banned Users------" + "\n";
        for (String username : bannedUsers) {
            bannedUsernames += username + "\n";
        }
        return bannedUsernames;
    }

    

    @RequestMapping(value = "/bot/{botName}", method = RequestMethod.PUT)
    public void addBot(@PathVariable("botName") String botName) {
        BotInfo[] botsInformation = rest.getForObject("http://localhost:8080/botsInformation", BotInfo[].class);
        for (BotInfo info : botsInformation) {
            if (info.name.equals(botName)) {
                botURLs.put(info.name, info.address);
                System.out.println("Bot: " + botName + " has been added");
                return;
            }
        }
        MessageInfo info = new MessageInfo(displayName, "Bot: " + botName + " could not be found");
        sendMessage(info);
    }

    @RequestMapping(value = "/bot/{botName}", method = RequestMethod.DELETE)
    public void removeBot(@PathVariable("botName") String botName) {
        String results = botURLs.remove(botName);
        // if map returns null it means the key did not have an association
        if (results == null) {
            MessageInfo info = new MessageInfo(displayName, "Bot: " + botName + " was not found and therefore could not be removed");
            sendMessage(info);
        } else {
            System.out.println();
            MessageInfo info = new MessageInfo(displayName, "Bot: " + botName + " has been removed");
            sendMessage(info);
        }
    }

    @RequestMapping(value = "/bots", method = RequestMethod.GET)
    public String listBots() {
        String bots = "";
        for (String botName : botURLs.keySet()) {
            bots += botName + "\n";   
        }
        return bots;
    }

    // TODO update the urls below to be more uniform
    @RequestMapping(value = "/message", method = RequestMethod.PUT)
    public void sendMessage(@RequestBody MessageInfo info) {
        // Get and update sequential id
        int messageId = currentId;
        currentId++;

        // Get data from message info and insert it into the collection
        Document document = new Document("sequenceId", messageId).append("username", info.username).append("message", info.message);
        collection.insertOne(document);
    }

    @RequestMapping(value = "/loginMessages", method = RequestMethod.GET)
    public LinkedList<MessageInfo> loginMessages() {
        return getMessages(currentId - LOGIN_MESSAGE_AMOUNT);
    }

    @RequestMapping(value = "/getMessages/{currentMessageId}", method = RequestMethod.GET)
    public LinkedList<MessageInfo> getMessages(@PathVariable("currentMessageId") int currentMessageId) {
        // Create Query
        BasicDBObject filter = new BasicDBObject();
        // get all messages which are greater than the current message id
        filter.append("sequenceId", new Document("$gt", currentMessageId));

        BasicDBObject fields = new BasicDBObject();
        fields.put("_id", 0);
        fields.put("sequenceId", 1);
        fields.put("username", 1);
        fields.put("message", 1);

        // TODO add orderby clause

        // Getting the iterator
        MongoCursor<Document> it = collection
            .find(filter)
            .projection(fields)
            .iterator();

        Gson gson = new Gson();
        LinkedList<MessageInfo> messages = new LinkedList<MessageInfo>();
        while (it.hasNext()) {
            MessageInfo message = gson.fromJson(it.next().toJson(), MessageInfo.class);
            messages.add(message);
        }
        return messages;
    }

    @RequestMapping(value = "/commandBot/{botName}", method = RequestMethod.PUT)
    public void commandBot(@PathVariable("botName") String botName, @RequestBody MessageInfo info) {
        BotCommand command = new BotCommand(responseAddress, info);
        if (botURLs.containsKey(botName)) {
            String botURL = botURLs.get(botName);
            rest.put(botURL + "/run", command);
        } else {
            throw new NoSuchBotException();
        }
    }

    // A method for getting a heart beat from server
    @RequestMapping(value = "/test", method = RequestMethod.PUT)
    public void test() {}
}

// Thread that checks Authentication sever to see if the following channel is on its registry
// If it is not found, it will add itself to it
class AuthServerChecker implements Runnable {
    private Thread thread;
    private String name;
    RestTemplate rest = new RestTemplate();

    public AuthServerChecker(String name) {
        this.name = name;
    }

    public void run() {
        while(true) {
            try{
                // this will not be 10 seconds in production
                TimeUnit.SECONDS.sleep(60);
                rest.getForObject("http://localhost:8080/channel/" + name, ChannelInfo.class);
            } catch (InterruptedException e) {
                System.out.println("Error occured while checking auth server registry");
            } catch (HttpClientErrorException e) {
                int statusCode = e.getRawStatusCode();
                switch (statusCode) {
                case 404:
                    System.out.println("Channel was not found in auth server, adding it back");
                    rest.put("http://localhost:8080/channel", Channel.info);
                    break;
                }
            } catch (Exception e) {
                System.out.println("Error occured while checking auth server registry");
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "Starting auth checker");
            thread.start();
        }
    }
}