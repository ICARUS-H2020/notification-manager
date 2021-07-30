package aero.icarus2020.services;

import aero.icarus2020.config.NotificationListenerConfig;
import aero.icarus2020.config.WebsocketsSenderConfig;
import aero.icarus2020.models.*;
import aero.icarus2020.producers.WebsocketsSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
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

    private TestRestTemplate testRestTemplate;

    @Before
    public void setUp() {
        testRestTemplate = new TestRestTemplate();
    }

    @Test
    public void getUserTest() {
        ResponseEntity<UserDao> response = testRestTemplate.
                getForEntity(icarusAPI, UserDao.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    public void getAssetTest() {
        ResponseEntity<AssetDao> response = testRestTemplate.
                getForEntity(icarusAPI, AssetDao.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOrganizationUsersTest() {
        ResponseEntity<UserDao[]> response = testRestTemplate.
                getForEntity(icarusAPI, UserDao[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOrganizationTest() {
        ResponseEntity<OrganizationDao> response = testRestTemplate.
                getForEntity(icarusAPI, OrganizationDao.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getAllOrganizationsTest() {
        ResponseEntity<OrganizationDao[]> response = testRestTemplate.
                getForEntity(icarusAPI, OrganizationDao[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getAllAuthorisecOrganizationsTest() {
        ResponseEntity<Long[]> response = testRestTemplate.
                getForEntity(icarusAPI, Long[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}