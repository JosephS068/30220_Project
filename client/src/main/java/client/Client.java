package client; 

import java.text.NumberFormat;
import java.util.LinkedList;

import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import core.MessageInfo;

public class Client {
    public static void main(String[] args) {
        MessageInfo info = new MessageInfo("New User", "Testing adding to db");
        RestTemplate rest = new RestTemplate();
        rest.put("http://localhost:8084/setQuoters", info);
    }
}