package channel;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
import core.ChannelCommand;
import core.CommandLineHelper;
import core.ChannelConfiguration;

@RestController
public class Channel {
    private final static int LOGIN_MESSAGE_AMOUNT = 20;
    private static int currentId = 0;
    private final static String ADMIN = "ADMIN";
    private final static String AUTHORIZED = "AUTHORIZED";
    private final static String BANNED = "BANNED";

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
    private static MongoCollection<Document> configurationDB;
    private static MongoCollection<Document> userStatusDB;
    private static MongoCollection<Document> botDB;
    private static MongoCollection<Document> messagesDB;

    private static RestTemplate rest = new RestTemplate();
    public static Console console = System.console();
    private static Gson gson = new Gson();

    private final static Map<String, String> botURLs = new HashMap<String, String>();
    // Create mongodb for this later
    private static ArrayList<String> authorizedUsers = new ArrayList<String>();
    private static ArrayList<String> bannedUsers = new ArrayList<String>();
    private static ArrayList<String> adminUsers = new ArrayList<String>();
    
    @PostConstruct
    public void init() {
        // Login to MongoDB
        mongo = new MongoClient("localhost", 27017);
        credential = MongoCredential.createCredential("admin", "Channel-Data", "admin".toCharArray());
        // Access database and get collection for message information
        database = mongo.getDatabase("Channel-Data");
        configurationDB = database.getCollection("Configuration");
        userStatusDB = database.getCollection("User-Information");
        botDB = database.getCollection("Bot-Information");
        messagesDB = database.getCollection("Message-Information");

        checkExistingConfigurations();

        // Send authentication server channel information
        info = new ChannelInfo(name, address, description);
        rest.put("http://authenticationServer:8080/channel", info);

        AuthServerChecker checker = new AuthServerChecker(name);
        checker.start();
    }

    public void checkExistingConfigurations() {
        FindIterable<Document> iterDoc = configurationDB.find();
        MongoCursor<Document> it = iterDoc.iterator();
        if (it.hasNext()) {
            System.out.println("This server already has config options would you like to use them?(y/n)");
            boolean useExisting = CommandLineHelper.responseYesNo();
            if (useExisting) {
                ChannelConfiguration config = gson.fromJson(it.next().toJson(), ChannelConfiguration.class);
                name = config.name;
                displayName = "{" + name + "}";
                address = config.address;
                responseAddress = address + "/message";
                description = config.description;
                requiresPin = config.requiresPin;
                pin = config.pin.toCharArray();
                adminUsers = getUserForStatus(ADMIN);
                bannedUsers = getUserForStatus(BANNED);
                authorizedUsers = getUserForStatus(AUTHORIZED);
                // Bots aren't always online, must be reconfigured everytime by admins
            } else {
                serverConfiguration();
            }
        } else {
            System.out.println("This server has not been configured before, please provide additional details");
            serverConfiguration();
        }
    }

    public ArrayList<String> getUserForStatus(String Status) {
        BasicDBObject filter = new BasicDBObject();
        filter.append("status", new Document("$eq", Status));

        BasicDBObject fields = new BasicDBObject();
        fields.put("_id", 0);
        fields.put("username", 1);

        FindIterable<Document> iterDoc = userStatusDB
            .find(filter)
            .projection(fields);

        MongoCursor<Document> it = iterDoc.iterator();
        ArrayList<String> users = new ArrayList<String>();
        while(it.hasNext()) {
            String user = it.next().get("username").toString();
            users.add(user);
        }
        return users;
    }

    // Configuration of server from user input
    public void serverConfiguration() {
        // Drop collections to make sure no values have been brought over
        userStatusDB.drop();
        messagesDB.drop();
        configurationDB.drop();

        database.createCollection("Configuration");
        database.createCollection("User-Information");
        database.createCollection("Message-Information");

        name = CommandLineHelper.getServerName();
        displayName = "{" + name + "}";

        System.out.println("What is the address of this server(Name:Port)?");
        address = "http://" + CommandLineHelper.noNullInput();
        // TODO take this line out its for testing only
        address = "http://localhost:8084";
        responseAddress = address + "/message";

        System.out.println("What is the description of this server?");
        description = CommandLineHelper.noNullInput();

        addBots();

        System.out.println("Would you like a pin for this server?(y/n)");
        requiresPin = CommandLineHelper.responseYesNo();
        if (requiresPin) {
            addPin();
        }

        String adminAccount;
        boolean validName;
        do {
            System.out.println("Name of admin user");
            adminAccount = CommandLineHelper.noNullInput();
            System.out.println("Is " + adminAccount + " the correct user?(y/n)");
            validName = CommandLineHelper.responseYesNo();
        } while (!validName);

        adminUsers.add(adminAccount);

        Document document = new Document("username", adminAccount).append("status", ADMIN);
        userStatusDB.insertOne(document);

        saveConfiguration();
        System.out.println("Thank you, the server will now finish start up");
    }

    public void addBots() {
        BotInfo[] botsInformation = rest.getForObject("http://authenticationServer:8080/botsInformation", BotInfo[].class);
        if (botsInformation.length == 0) {
            System.out.println("No bots found skipping this section of configuration");
            return;
        }

        System.out.println("Which bots would you like to be available for this server?");
        System.out.println("type all for all");
        System.out.println("type none for none");
        System.out.println("or list the names seperated by spaces");

        String botList = rest.getForObject("http://authenticationServer:8080/botsList", String.class);
        System.out.println("");
        System.out.println("Currently available bots");
        System.out.println("----------------------------------------------------");
        System.out.print(botList);
        System.out.println("----------------------------------------------------");
        String bots = CommandLineHelper.noNullInput();
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
        boolean addBots = CommandLineHelper.responseYesNo();
        if (!addBots) {
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

    public void saveConfiguration() {
        Document document = new Document("name", name)
        .append("address", address)
        .append("description", description)
        .append("requiresPin", requiresPin);
        if(pin != null) {
            document.append("pin", new String(pin));
        } else {
            document.append("pin", " ");
        }
        configurationDB.insertOne(document);
    }

    public void authorizationCheck(String username) {
        if (requiresPin && !authorizedUsers.contains(username)) {
            throw new NotAuthorizedException();
        }
    }

    public void banCheck(String username) {
        if (bannedUsers.contains(username)) {
            throw new BannedUserException();
        }
    }

    public void adminCheck(String username) {
        if (!adminUsers.contains(username)) {
            throw new RequiresAdminException();
        }
    }

    @RequestMapping(value = "/authenticate/{user}", method = RequestMethod.GET)
    public Boolean requiresPin(@PathVariable("user") String username) {
        banCheck(username);
        if (authorizedUsers.contains(username)) {
            return new Boolean(false);
        } else {
            return new Boolean(requiresPin);
        }
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
            Document document = new Document("username", username).append("status", AUTHORIZED);
            userStatusDB.insertOne(document);
        } else {
            throw new InvalidPinException();
        }
    }

    @RequestMapping(value = "/ban", method = RequestMethod.PUT)
    public void banUser(@RequestBody ChannelCommand command) {
        adminCheck(command.issuer);
        String banMessage;
        if (!bannedUsers.contains(command.subject)) {
            bannedUsers.add(command.subject);
            banMessage = command.subject + " just got clapped";
            Document document = new Document("username", command.subject).append("status", BANNED);
            userStatusDB.insertOne(document);
        } else {
            banMessage = command.subject + " just got clapped again, a double ban isn't a thing though";
        }
        MessageInfo info = new MessageInfo(displayName, banMessage);
        sendMessage(info);        
    }

    @RequestMapping(value = "/ban/{issuer}/{subject}", method = RequestMethod.DELETE)
    public void unbanUser(@PathVariable("issuer") String issuer, @PathVariable("subject") String subject) {
        adminCheck(issuer);
        String unbanMessage;
        if (!bannedUsers.contains(subject)) {
            unbanMessage = subject + " was supposed to be unbanned, it appears they are being good and were not banned to begin with!";
        } else {
            bannedUsers.remove(subject);
            Document document = new Document("username", subject).append("status", BANNED);
            userStatusDB.deleteOne(document);
            unbanMessage = subject + " has risen from the dead, aka unbanned";
        }
        MessageInfo info = new MessageInfo(displayName, unbanMessage);
        sendMessage(info);
    }

    @RequestMapping(value = "/ban", method = RequestMethod.GET)
    public String showBans() {
        String bannedUsernames = "------Banned Users------" + "\n";
        for (String username : bannedUsers) {
            bannedUsernames += username + "\n";
        }
        return bannedUsernames;
    }

    @RequestMapping(value = "/admin", method = RequestMethod.PUT)
    public void giveAdmin(@RequestBody ChannelCommand command) {
        adminCheck(command.issuer);
        String adminMessage;
        if (!adminUsers.contains(command.subject)) {
            adminUsers.add(command.subject);
            Document document = new Document("username", command.subject).append("status", ADMIN);
            userStatusDB.insertOne(document);
            adminMessage = command.subject + " has been blessed with the power of admin";
        } else {
            adminMessage = command.subject + " just got double admin, this doesn't do anything, but good job";
        }
        MessageInfo info = new MessageInfo(displayName, adminMessage);
        sendMessage(info);
    }

    @RequestMapping(value = "/admin/{issuer}/{subject}", method = RequestMethod.DELETE)
    public void takeAdmin(@PathVariable("issuer") String issuer, @PathVariable("subject") String subject) {
        adminCheck(issuer);
        String adminMessage;
        if (!adminUsers.contains(subject)) {
            adminMessage = subject + " wasn't an admin but someone wanted to take admin away from you, be careful out there";
        } else {
            adminUsers.remove(subject);
            Document document = new Document("username", subject).append("status", ADMIN);
            userStatusDB.deleteOne(document);
            adminMessage = subject + " went back on his ninja's way and lost admin";
        }
        MessageInfo info = new MessageInfo(displayName, adminMessage);
        sendMessage(info);
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public String showAdmins() {
        String adminUsernames = "------Admin Users------" + "\n";
        for (String username : adminUsers) {
            adminUsernames += username + "\n";
        }
        return adminUsernames;
    }

    @RequestMapping(value = "/bot", method = RequestMethod.PUT)
    public void addBot(@RequestBody ChannelCommand command) {
        adminCheck(command.issuer);
        BotInfo[] botsInformation = rest.getForObject("http://authenticationServer:8080/botsInformation", BotInfo[].class);
        for (BotInfo info : botsInformation) {
            if (info.name.equals(command.subject)) {
                botURLs.put(info.name, info.address);
                MessageInfo messageInfo = new MessageInfo(displayName, "Bot: " + command.subject + " has been added");
                sendMessage(messageInfo);
                return;
            }
        }
        MessageInfo info = new MessageInfo(displayName, "Bot: " + command.subject + " could not be found");
        sendMessage(info);
    }

    @RequestMapping(value = "/bot/{issuer}/{subject}", method = RequestMethod.DELETE)
    public void removeBot(@PathVariable("issuer") String issuer, @PathVariable("subject") String subject) {
        adminCheck(issuer);
        if (botURLs.containsKey(subject)) {
            botURLs.remove(subject);
            MessageInfo info = new MessageInfo(displayName, "Bot: " + subject + " has been removed");
            sendMessage(info);
        } else {
            
            MessageInfo info = new MessageInfo(displayName, "Bot: " + subject + " was not found and therefore could not be removed");
            sendMessage(info);
        }
    }

    @RequestMapping(value = "/bots", method = RequestMethod.GET)
    public String listBots() {
        String bots = "------Current Bots------\n";
        for (String botName : botURLs.keySet()) {
            bots += botName + "\n";   
        }
        return bots;
    }

    @RequestMapping(value = "/message", method = RequestMethod.PUT)
    public void sendMessage(@RequestBody MessageInfo info) {
        // Get and update sequential id
        int messageId = currentId;
        currentId++;

        // Get data from message info and insert it into the collection
        Document document = new Document("sequenceId", messageId).append("username", info.username).append("message", info.message);
        messagesDB.insertOne(document);
    }

    @RequestMapping(value = "/message", method = RequestMethod.GET)
    public LinkedList<MessageInfo> loginMessages() {
        return getMessages(currentId - LOGIN_MESSAGE_AMOUNT);
    }

    @RequestMapping(value = "/message/{currentMessageId}", method = RequestMethod.GET)
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

        // Getting the iterator
        MongoCursor<Document> it = messagesDB
            .find(filter)
            .projection(fields)
            .iterator();

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
                rest.getForObject("http://authenticationServer:8080/channel/" + name, ChannelInfo.class);
            } catch (InterruptedException e) {
                System.out.println("Error occured while checking auth server registry");
            } catch (HttpClientErrorException e) {
                int statusCode = e.getRawStatusCode();
                switch (statusCode) {
                case 404:
                    System.out.println("Channel was not found in auth server, adding it back");
                    rest.put("http://authenticationServer:8080/channel", Channel.info);
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