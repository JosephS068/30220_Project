package authenticationServer;

import java.util.Map;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.RestController;

import authenticationServer.NoSuchChannelException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import core.ChannelInfo;

@RestController
public class AuthenticationServer {
    private final static Map<String, ChannelInfo> channelsInfo = new HashMap<String, ChannelInfo>();
    @PostConstruct
    public void init() {
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