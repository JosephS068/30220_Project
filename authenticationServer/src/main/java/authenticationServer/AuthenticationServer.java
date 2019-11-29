package channel;
import com.google.gson.Gson;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import javax.annotation.PostConstruct;

import org.bson.Document;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.ChannelInfo;
import core.MessageInfo;

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
        return channelsInfo.get(channelName);
    }
}