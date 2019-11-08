package channel;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.Iterator;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import core.MessageInfo;

@RestController
public class Channel {
    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    public void getApplications(@RequestBody MessageInfo info) {
        System.out.println(info.username);
        System.out.println(info.message);
        MongoClient mongo = new MongoClient("localhost", 27017);
        MongoCredential credential = MongoCredential.createCredential("sampleUser", "myDb", "password".toCharArray());
        System.out.println("Connected to the database successfully");
        MongoDatabase database = mongo.getDatabase("myDb");
        MongoCollection<Document> collection = database.getCollection("sampleCollection");
        Document document = new Document("User", info.username).append("Message", info.message);
        collection.insertOne(document);
        System.out.println("Document inserted successfully");
    }

    // Testing Rest Functionality
    @RequestMapping(value = "/test", method = RequestMethod.PUT)
    public void getApplications(@RequestBody String test) {
        System.out.println(test);
    }
}