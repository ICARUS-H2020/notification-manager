package aero.icarus2020.services;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * This class tests that the apis are available to the application.
 * Each method runs only if the application is compiled with the command "-Dtest-groups=integration-tests"
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "test-groups", values = {"integration-tests"})
public class AsyncServiceIntegrationTest {

    @Autowired
    private AsyncService asyncService;

    @Value("${icarus.api.entry}")
    private String icarusAPI;

    private TestRestTemplate testRestTemplate;

    @Before
    public void setUp() {
        testRestTemplate = new TestRestTemplate();
    }

    @Test
    public void sendEmailTest() {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        JSONObject mailCredentials = new JSONObject();
        mailCredentials.put("username", "mail");
        mailCredentials.put("password", "pxF2xasv7!6prETVaCQ2");

        HttpEntity<String> request =
                new HttpEntity<String>(mailCredentials.toString(), httpHeaders);

        ResponseEntity<String> responseEntity = testRestTemplate.exchange(icarusAPI + "/login", HttpMethod.POST, request, String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // get cookie and send test email
        HttpEntity<String> response = testRestTemplate.exchange(icarusAPI + "/login", HttpMethod.POST, request, String.class);

        HttpHeaders headers = response.getHeaders();

        List<String> cookies = headers.get("Set-Cookie");

        mailCredentials = new JSONObject();

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", cookies.get(0));

        mailCredentials.put("to", "test_email");
        mailCredentials.put("subject", "ICARUS: test email");
        mailCredentials.put("text", "Testing Email Module");

        request = new HttpEntity<String>(mailCredentials.toString(), headers);
        responseEntity = testRestTemplate.exchange(icarusAPI + "/mail", HttpMethod.POST, request, String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    }
}