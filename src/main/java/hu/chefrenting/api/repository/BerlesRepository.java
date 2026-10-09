package hu.chefrenting.api.repository;

import hu.chefrenting.api.entity.Berles;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BerlesRepository extends JpaRepository<Berles, Integer> {

    /**
     * Is there a rental of the same chef that overlaps [start, end] (all dates inclusive)?
     * Overlap: existing.start <= newEnd AND existing.end >= newStart; evaluated by the database.
     */
    boolean existsByChefIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            int chefId, LocalDate newEnd, LocalDate newStart);
}
