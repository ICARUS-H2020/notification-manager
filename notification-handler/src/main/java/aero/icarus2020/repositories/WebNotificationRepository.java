package aero.icarus2020.repositories;

import aero.icarus2020.models.WebNotification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebNotificationRepository extends CrudRepository<WebNotification, Long> {

}
