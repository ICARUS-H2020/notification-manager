package aero.icarus2020.respositories;

import aero.icarus2020.models.WebNotification;
import aero.icarus2020.repositories.WebNotificationRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Optional;

/** This test class will run only if the with the command "-Dtest-groups=integration-tests"
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "test-groups", values = {"integration-tests"})
public class WebNotificationRepositoryIntegrationTest {

    private WebNotification webNotification;

    private long _notif_id;

    @Autowired
    private WebNotificationRepository webNotificationRepository;

    /**
     * This method creates a fake webNotification before executing any tests
     */
    @Before
    public void setup() {
        webNotification = new WebNotification();
        webNotification.setRecipient_id(40);
        webNotification.set_created_on(new Timestamp(System.currentTimeMillis()));
        String notification_type = "Test";
        webNotification.setNotif_type(notification_type);
    }

    /**
     * This method checks that it will return either a EmptyResultDataAccessException exception either an empty
     * response since the notification with id "-1" does not exist.
     */
    @Test
    public void findByIdNotValidTest() {
        try {
            Optional<WebNotification> foundWebNotification = webNotificationRepository.findById((long) -1);
            assertThat(foundWebNotification.isPresent()).isEqualTo(false);
        } catch (EmptyResultDataAccessException ignored) {
        }
    }

    /**
     * This method saves the testing webNotification and tests that it was indeed saved.
     */
    @Test
    public void findByIdValidTest() {
        WebNotification wn = webNotificationRepository.save(webNotification);

        assertNotNull(wn);
        Optional<WebNotification> foundWebNotification = webNotificationRepository.findById(wn.get_notif_id());

        assertThat(foundWebNotification.isPresent()).isEqualTo(true);

        this._notif_id = wn.get_notif_id();
    }

    /**
     * This method deletes the testing webNotification before exiting
     */
    @After
    public void cleanUp() {
        try {
            webNotificationRepository.deleteById(_notif_id);
        } catch (EmptyResultDataAccessException ignored) {
        }
    }

}