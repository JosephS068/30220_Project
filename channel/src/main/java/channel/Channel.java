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
import java.io.Console;

import javax.annotation.PostConstruct;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.BotInfo;
import core.ChannelInfo;
import core.MessageInfo;

@RestController
public class Channel {
    private final static int LOGIN_MESSAGE_AMOUNT = 20;
    private static int currentId = 0;

    private static String name;
    private static String address;
    private static String responseAddress;
    private static String description;

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    private static RestTemplate rest = new RestTemplate();
    public static Console console = System.console();

    private final static Map<String, String> botURLs = new HashMap<String, String>();
    
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
        ChannelInfo info = new ChannelInfo(name, address, description);
        rest.put("http://localhost:8080/channel/", info);

        // Just testing resets collection on start up
        collection.drop();
        database.createCollection("Message-Information");
    }

    public void getServerInformation() {
        // check mongo db first
        // Configuration of server from user input
        System.out.println("This server has not been configured before, please provide additional details");
        System.out.println("What is the name of this server?");
        name = console.readLine();

        System.out.println("What is the address of this server(Name:Port)?");
        address = "http://" + console.readLine();
        // TODO take this line out its for testing only
        address = "http://localhost:8084";
        responseAddress = address + "/message";

        System.out.println("What is the description of this server?");
        description = console.readLine();

        addBots();
                
        // System.out.println("Does this server need a pin?");
        // System.out.println("Please enter pin");
        // System.out.println("validate pin");
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
            // TODO this is starting look like call back hell, fix this
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
        System.out.println("Bot: " + botName + " could not be found");
    }

    //Error handle
    @RequestMapping(value = "/bot/{botName}", method = RequestMethod.DELETE)
    public void removeBot(@PathVariable("botName") String botName) {
        String results = botURLs.remove(botName);
        // if map returns null it means the key did not have an association
        if (results == null) {
            System.out.println("Bot: " + botName + " was not found and therefore could not be removed");
        } else {
            System.out.println("Bot: " + botName + " has been removed");
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
        System.out.println("Document inserted");
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