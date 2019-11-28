package channel;
import com.google.gson.Gson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import java.util.LinkedList;

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

import core.MessageInfo;

@RestController
public class Channel {
    private final static int LOGIN_MESSAGE_AMOUNT = 20;
    private final static String address = "http://localhost:8084";
    private final static String responseAddress = address + "/message";
    private static int currentId = 0;

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;
    
    @PostConstruct
    public void init() {
        System.out.println("TEST");
        // Login to MongoDB
        mongo = new MongoClient("localhost", 27017);
        credential = MongoCredential.createCredential("admin", "Channel-Data", "admin".toCharArray());
        // Access database and get collection for message information
        database = mongo.getDatabase("Channel-Data");
        collection = database.getCollection("Message-Information");

        // Just testing resets collection on start up
        collection.drop();
        database.createCollection("Message-Information");
    }

    @RequestMapping(value = "/message", method = RequestMethod.PUT)
    public void sendMessage(@RequestBody MessageInfo info) {
        // Get sequential id and update id
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

    @RequestMapping(value = "/commandBot/{bot}", method = RequestMethod.PUT)
    public void commandBot(@RequestBody MessageInfo info) {
        // @PathVariable("bot") String bot,
        RestTemplate rest = new RestTemplate();
        // here we find the proper bot address to send to
        rest.put("http://localhost:8088/run", info);
    }
}