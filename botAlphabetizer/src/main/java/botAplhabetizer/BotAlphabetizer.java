package botAlphabetizer;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.BotInfo;
import core.MessageInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

@RestController
public class BotAlphabetizer {
    RestTemplate rest = new RestTemplate();
    private final static String name = "Alphabetizer";
    private final static String displayName = "[" + name + "]";
    private final static String address = "http://localhost:8090";
    public final static BotInfo info = new BotInfo(name, address);

    @PostConstruct
    public void init() {
        // Send authentication server bot information
        rest.put("http://localhost:8080/bot", info);
        AuthServerChecker checker = new AuthServerChecker(name);
        checker.start();
    }

    @RequestMapping(value = "/run", method = RequestMethod.PUT)
    public void orderWords(@RequestBody BotCommand command) {
        MessageInfo info = command.getInfo();
        ArrayList<String> parameters = new ArrayList<String>(Arrays.asList(info.message.split(" ")));
        Collections.sort(parameters);
        String sortedMessage = command.info.username + "\'s sorted message: ";
        for (String word : parameters) {
            sortedMessage += word + " ";
        }
        MessageInfo response = new MessageInfo(displayName, sortedMessage);
        rest.put(command.responseAddress, response);
    }

    // A method for getting a heart beat from server
    @RequestMapping(value = "/test", method = RequestMethod.PUT)
    public void test() {}
}

// Thread that checks Authentication sever to see if the following bot is on its
// registry
// If it is not found, it will add itself to it
class AuthServerChecker implements Runnable {
    private Thread thread;
    private String name;
    RestTemplate rest = new RestTemplate();

    public AuthServerChecker(String name) {
        this.name = name;
    }

    public void run() {
        while (true) {
            try {
                // this will not be 10 seconds in production
                TimeUnit.SECONDS.sleep(60);
                rest.getForObject("http://localhost:8080/bot/" + name, BotInfo.class);
            } catch (InterruptedException e) {
                System.out.println("Error occured while checking auth server registry");
            } catch (HttpClientErrorException e) {
                int statusCode = e.getRawStatusCode();
                switch (statusCode) {
                case 404:
                    System.out.println("Bot was not found in auth server, adding it back");
                    rest.put("http://localhost:8080/bot", BotAlphabetizer.info);
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