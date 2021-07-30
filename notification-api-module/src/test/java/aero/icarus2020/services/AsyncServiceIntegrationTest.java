package aero.icarus2020.services;

import aero.icarus2020.models.UserDao;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Value("${icarus.api.token}")
    private String icarusAPIToken;

    private TestRestTemplate testRestTemplate;

    @Before
    public void setUp() {
        testRestTemplate = new TestRestTemplate();
    }

    @Test
    public void isAssetValidTest() {
        ResponseEntity<String> response = testRestTemplate.
                getForEntity(icarusAPI, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    public void isAuthorisedOrganizationTest() {
        ResponseEntity<String> response = testRestTemplate.
                getForEntity(icarusAPI, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOrganizationIdTest() {
        ResponseEntity<UserDao> response = testRestTemplate.
                getForEntity(icarusAPI, UserDao.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Ignore
    public void checkTokenTest() {
        String token = "*";
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", "auth_token=" + token);
        HttpEntity requestEntity = new HttpEntity(null, requestHeaders);
        ResponseEntity<String> response = testRestTemplate.exchange(icarusAPIToken, HttpMethod.GET, requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
