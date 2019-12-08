package botTimer;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.BotInfo;
import core.MessageInfo;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

@RestController
public class BotTimer {
    RestTemplate rest = new RestTemplate();
    private final static String name = "Timer";
    private final static String displayName = "[" + name + "]";
    private final static String address = "http://botTimer:8088";
    public final static BotInfo info = new BotInfo(name, address);

    @PostConstruct
    public void init() {
        // Send authentication server bot information
        rest.put("http://authenticationServer:8080/bot", info);
        AuthServerChecker checker = new AuthServerChecker(name);
        checker.start();
    }

    @RequestMapping(value = "/run", method = RequestMethod.PUT)
    public void startTimer(@RequestBody BotCommand command) {       
        try {
            String responseAddress = command.getResponseAddress();
            MessageInfo info = command.getInfo();
            String[] parameters = info.message.split(" ");
            String durationString = parameters[0];
            int duration = Integer.parseInt(durationString);
            String units = parameters[1];
            Timer thread = new Timer(displayName, duration, units, info, responseAddress);
            thread.start();
        } catch (Exception e) {
            String errorMessage = "Error in parameters, should be !Timer [duraction] [Units]";
            MessageInfo response = new MessageInfo(displayName, errorMessage);
            rest.put(command.responseAddress, response);
        } 
    }

    // A method for getting a heart beat from server
    @RequestMapping(value = "/test", method = RequestMethod.PUT)
    public void test() {}
}

class Timer implements Runnable {
    private Thread thread;
    private String displayName;
    private int duration;
    private String units;
    private MessageInfo info;
    private String responseAddress;
    RestTemplate rest = new RestTemplate();

    public Timer(String displayName, int duration, String units, MessageInfo info, String responseAddress) {
        this.displayName = displayName;
        this.duration = duration;
        this.units = units;
        this.info = info;
        this.responseAddress = responseAddress;
    }

    public void run() {
        try {
            String startMessage = "Starting " + info.username + "\'s timer for " + duration + " " + units;
            MessageInfo startResponse = new MessageInfo(displayName, startMessage);
            rest.put(responseAddress, startResponse);

            boolean parameterError = false;
            switch (units.toLowerCase()) {
                case "days":
                case "day":
                    TimeUnit.DAYS.sleep(duration);
                    break;
                case "hours":
                case "hour":
                    TimeUnit.HOURS.sleep(duration);
                    break;
                case "minutes":
                case "minute":
                    TimeUnit.MINUTES.sleep(duration);
                    break;
                case "seconds":
                case "second":
                    TimeUnit.SECONDS.sleep(duration);
                    break;
                default:
                    parameterError = true;
                    break;
            }
            String message;
            if (parameterError) {
                message = info.username
                        + " tried to invent a new unit of time which I don't understand, don't do this again.";
            } else {
                message = info.username + "\'s timer for " + duration + " " + units + " has now completed";
            }
            MessageInfo response = new MessageInfo(displayName, message);
            rest.put(responseAddress, response);
        } catch (InterruptedException e) {
            String errorMessage = "Dear, " + info.username + " an error has occured while trying to process your request please try again later";
            MessageInfo response = new MessageInfo(displayName, errorMessage);
            rest.put(responseAddress, response);
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "Run timmer");
            thread.start();
        }
    }
}

// Thread that checks Authentication sever to see if the following bot is on its registry
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
                rest.getForObject("http://authenticationServer:8080/bot/" + name, BotInfo.class);
            } catch (InterruptedException e) {
                System.out.println("Error occured while checking auth server registry");
            } catch (HttpClientErrorException e) {
                int statusCode = e.getRawStatusCode();
                switch (statusCode) {
                case 404:
                    System.out.println("Bot was not found in auth server, adding it back");
                    rest.put("http://authenticationServer:8080/bot", BotTimer.info);
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