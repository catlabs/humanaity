package eu.catlabs.humanaity.event.infrastructure.persistence;

import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findTopByCityIdAndTickAndEventCategoryOrderBySequenceInTickDesc(
            Long cityId,
            Long tick,
            EventCategory eventCategory
    );

    List<Event> findByCityIdOrderByTickAscSequenceInTickAscIdAsc(Long cityId);

    List<Event> findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(Long cityId, EventType eventType);
}
