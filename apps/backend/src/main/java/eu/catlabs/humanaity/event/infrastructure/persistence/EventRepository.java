package eu.catlabs.humanaity.event.infrastructure.persistence;

import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

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

    List<Event> findByCityIdAndTickBetweenOrderByTickAscSequenceInTickAscIdAsc(Long cityId, Long fromTick, Long toTick);

    long deleteByCityId(Long cityId);

    @Query("""
            select e from Event e
            where e.city.id = :cityId
              and :humanId member of e.actorIds
              and e.tick between :fromTick and :toTick
            order by e.tick asc, e.sequenceInTick asc, e.id asc
            """)
    List<Event> findHumanEventsInTickWindow(
            @Param("cityId") Long cityId,
            @Param("humanId") Long humanId,
            @Param("fromTick") Long fromTick,
            @Param("toTick") Long toTick
    );
}
