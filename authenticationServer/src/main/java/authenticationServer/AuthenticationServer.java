package authenticationServer;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import authenticationServer.NoSuchChannelException;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import core.ChannelInfo;
import core.BotInfo;

@RestController
public class AuthenticationServer {
    private static RestTemplate rest = new RestTemplate();
    private final static Map<String, ChannelInfo> channelsInfo = new HashMap<String, ChannelInfo>();
    private final static Map<String, BotInfo> botsInfo = new HashMap<String, BotInfo>();

    @PostConstruct
    public void init() {
        HeartBeatChecker checker = new HeartBeatChecker();
        checker.start();
    }

    @RequestMapping(value = "/channel", method = RequestMethod.PUT)
    public void channelList(@RequestBody ChannelInfo info) {
        channelsInfo.put(info.name, info);
    }

    @RequestMapping(value = "/channel/{channelName}", method = RequestMethod.GET)
    public ChannelInfo getChannelInformation(@PathVariable("channelName") String channelName) {
        if (channelsInfo.containsKey(channelName)) {
            return channelsInfo.get(channelName);
        } else {
            throw new NoSuchChannelException();
        }
    }

    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    public String channelList() {
        String channelsMessage = "";
        for (ChannelInfo info : channelsInfo.values()) {
            channelsMessage += info.name + "\n";
        }
        if(channelsInfo.size() == 0) {
            channelsMessage = "No channels are available at this moment \n";
        }
        return channelsMessage;
    }

    @RequestMapping(value = "/bot", method = RequestMethod.PUT)
    public void botList(@RequestBody BotInfo info) {
        botsInfo.put(info.name, info);
    }
    
    @RequestMapping(value = "/bot/{botName}", method = RequestMethod.GET)
    public BotInfo getBotInformation(@PathVariable("botName") String botName) {
        if (botsInfo.containsKey(botName)) {
            return botsInfo.get(botName);
        } else {
            throw new NoSuchBotException();
        }
    }

    @RequestMapping(value = "/botsList", method = RequestMethod.GET)
    public String botList() {
        String botsMessage = "";
        for (BotInfo info : botsInfo.values()) {
            botsMessage += info.name + "\n";
        }
        if(botsInfo.size() == 0) {
            botsMessage = "No bots are available at this moment \n";
        }
        return botsMessage;
    }

    @RequestMapping(value = "/botsInformation", method = RequestMethod.GET)
    public BotInfo[] botsInformation() {
        BotInfo[] infoArray = botsInfo.values().toArray(new BotInfo[0]);
        return infoArray;
    }

    public static void checkChannelsAvailable() {
        for (ChannelInfo info : channelsInfo.values()) {
            try {
                rest.put(info.address + "/test", null);
            } catch (Exception e) {
                // Exception occured while testing, assume server is not active remove from list
                channelsInfo.remove(info.name);
            }
        }
    }

    public static void checkBotsAvailable() {
        for (BotInfo info : botsInfo.values()) {
            try {
                rest.put(info.address + "/test", null);
            } catch (Exception e) {
                // Exception occured while testing, assume server is not active remove from list
                botsInfo.remove(info.name);
            }
        }
    }
}

// Thread which prints result from broker
class HeartBeatChecker implements Runnable {
    private Thread thread;

    public HeartBeatChecker() {
    }

    public void run() {
        try {
            while(true) {
                AuthenticationServer.checkBotsAvailable();
                AuthenticationServer.checkChannelsAvailable();
                TimeUnit.SECONDS.sleep(10);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception occured, no longer checking heart beats");
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "Check channels");
            thread.start();
        }
    }
}