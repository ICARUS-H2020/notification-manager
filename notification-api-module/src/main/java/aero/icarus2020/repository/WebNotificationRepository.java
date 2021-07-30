package aero.icarus2020.repository;

import aero.icarus2020.models.WebNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebNotificationRepository extends CrudRepository<WebNotification, Long>, PagingAndSortingRepository<WebNotification, Long> {
    @Query("select n from notifications n where n.recipient_id=?1")
    Page<WebNotification> findByRecipientId(long recipient_id, Pageable pageable);

    @Query("select n from notifications n where n.recipient_id=?1 AND seen_on=null")
    List<WebNotification> unseenByRecipientId(long recipient_id);

    @Query("select n from notifications n where n.recipient_id=?1 AND seen_on=null")
    Page<WebNotification> unseenByRecipientId(long recipient_id, Pageable pageable);

    @Query("select n from notifications n where n.recipient_id=?1 AND seen_on!=null")
    List<WebNotification> seenByRecipientId(long recipient_id);

    @Query("select n from notifications n where n.recipient_id=?1 AND seen_on!=null")
    Page<WebNotification> seenByRecipientId(long recipient_id, Pageable pageable);

    @Query("select n from notifications n where n._notif_id=?1 AND n.recipient_id=?2")
    WebNotification findByNotifId(long notif_id, long recipient_id);
}
