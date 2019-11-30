package authenticationServer;

import java.util.Map;
import java.util.HashMap;
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

@RestController
public class AuthenticationServer {
    private final static Map<String, ChannelInfo> channelsInfo = new HashMap<String, ChannelInfo>();
    private static RestTemplate rest = new RestTemplate();

    @PostConstruct
    public void init() {
        ChannelChecker checker = new ChannelChecker();
        checker.start();
    }

    public static void checkChannelsAvailable() {
        for (ChannelInfo info : channelsInfo.values()) {
            try {
                rest.getForObject(info.address + "/test", void.class);
            } catch (Exception e) {
                // Exception occured while testing, assume server is not active remove from list
                channelsInfo.remove(info.name);
            }
        }

    }

    @RequestMapping(value = "/channel/join", method = RequestMethod.PUT)
    public void channelList(@RequestBody ChannelInfo info) {
        channelsInfo.put(info.name, info);
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
    
    @RequestMapping(value = "/channel/info/{channelName}", method = RequestMethod.GET)
    public ChannelInfo getChannelInformation(@PathVariable("channelName") String channelName) {
        if (channelsInfo.containsKey(channelName)) {
            return channelsInfo.get(channelName);
        } else {
            throw new NoSuchChannelException();
        }
    }
}

// Thread which prints result from broker
class ChannelChecker implements Runnable {
    private Thread thread;

    public ChannelChecker() {
    }

    public void run() {
        try {
            while(true) {
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