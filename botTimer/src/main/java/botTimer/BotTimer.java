package botTimer;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.MessageInfo;

import java.util.concurrent.TimeUnit;

@RestController
public class BotTimer {
    RestTemplate rest = new RestTemplate();
    private final static String name = "[TimerBot]";

    @RequestMapping(value = "/run", method = RequestMethod.PUT)
    public void startTimer(@RequestBody BotCommand command) {        
        try {
            String responseAddress = command.getResponseAddress();
            MessageInfo info = command.getInfo();
            String[] parameters = info.message.split(" ");
            String durationString = parameters[0];
            int duration = Integer.parseInt(durationString);
            String units = parameters[1];
            Timer thread = new Timer(name, duration, units, info, responseAddress);
            thread.start();
        } catch (Exception e) {
            rest.put(command.responseAddress, "Error in parameters, should be !Timer [duraction] [Units]");
        } 
    }
}

// Thread which prints result from broker
class Timer implements Runnable {
    private Thread thread;
    private String name;
    private int duration;
    private String units;
    private MessageInfo info;
    private String responseAddress;
    RestTemplate rest = new RestTemplate();

    public Timer(String name, int duration, String units, MessageInfo info, String responseAddress) {
        this.name = name;
        this.duration = duration;
        this.units = units;
        this.info = info;
        this.responseAddress = responseAddress;
    }

    public void run() {
        try {
            String errorMessage = "Starting " + info.username + "\'s timer for " + duration + " " + units;
            MessageInfo response = new MessageInfo(name, errorMessage);
            rest.put(responseAddress, response);

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
            MessageInfo response = new MessageInfo(name, message);
            rest.put(responseAddress, response);
        } catch (InterruptedException e) {
            String errorMessage = "Dear, " + info.username + " an error has occured while trying to process your request please try again later";
            MessageInfo response = new MessageInfo(name, errorMessage);
            rest.put(responseAddress, response);
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "Print Results");
            thread.start();
        }
    }
}