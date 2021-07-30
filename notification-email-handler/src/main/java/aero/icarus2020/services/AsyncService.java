package aero.icarus2020.services;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Service
public class AsyncService {

    @Value("${icarus.api.entry}")
    private String icarusAPI;

    @Autowired
    private RestTemplate restTemplate;

    private List<String> login() {
        String loginURI = icarusAPI;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        JSONObject mailCredentials = new JSONObject();
        mailCredentials.put("username", "*");
        mailCredentials.put("password", "*");

        HttpEntity<String> request =
                new HttpEntity<String>(mailCredentials.toString(), httpHeaders);

        HttpEntity<String> response =
                restTemplate.exchange(loginURI, HttpMethod.POST, request, String.class);

        HttpHeaders headers = response.getHeaders();
        List<String> cookies = headers.get("Set-Cookie");

        System.out.println(response);

        return cookies;

    }

    @Async("asyncExecutor")
    public void sendMail(String to, String subject, String body) throws IOException {

        List<String> cookies;
        cookies = this.login();

        String mailURI = icarusAPI;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", cookies.get(0));

        String filename = "/home/EmailTemplate.html";

        String content = new String(Files.readAllBytes(Paths.get(filename)));

        Document html = Jsoup.parse(content);
        html.body().getElementById("body").text(body);
        html.body().getElementById("title").text(subject);

        JSONObject mailCredentials = new JSONObject();
        mailCredentials.put("to", to);
        mailCredentials.put("subject", "ICARUS: " + subject);
        mailCredentials.put("text", html.toString());

        HttpEntity<String> request =
                new HttpEntity<String>(mailCredentials.toString(), headers);

        HttpEntity<String> response =
                restTemplate.exchange(mailURI, HttpMethod.POST, request, String.class);
    }

    public String getIcarusAPI() {
        return icarusAPI;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
