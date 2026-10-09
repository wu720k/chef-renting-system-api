package hu.chefrenting.api.service;

import hu.chefrenting.api.dto.BerlesRequest;
import hu.chefrenting.api.entity.Berles;
import hu.chefrenting.api.exception.InvalidRentalException;
import hu.chefrenting.api.exception.RentalNotFoundException;
import hu.chefrenting.api.repository.BerlesRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BerlesService {
    public static final int MIN_DAYS = 3;
    public static final int MAX_DAYS = 14;

    private final BerlesRepository repository;

    public BerlesService(BerlesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Berles> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Berles findById(int id) {
        return repository.findById(id).orElseThrow(() -> new RentalNotFoundException(id));
    }

    /** Validates the business rules (new rentals only), then saves; the id is database-generated. */
    @Transactional
    public Berles create(BerlesRequest request) {
        validate(request, LocalDate.now());
        Berles rental = new Berles(request.uid(), request.chefId(), request.startDate(), request.endDate(),
                request.dailyRate(), request.baseFee());
        return repository.save(rental);
    }

    @Transactional
    public void delete(int id) {
        if (!repository.existsById(id)) {
            throw new RentalNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private void validate(BerlesRequest r, LocalDate today) {
        if (r.dailyRate() <= 0) {
            throw new InvalidRentalException("A napi díjnak (dailyRate) pozitívnak kell lennie.");
        }
        if (r.baseFee() < 0) {
            throw new InvalidRentalException("Az alapdíj (baseFee) nem lehet negatív.");
        }
        if (r.startDate().isBefore(today.plusDays(1))) {
            throw new InvalidRentalException(
                    "A bérlés kezdete legkorábban holnap lehet (a mai vagy múltbeli kezdőnap érvénytelen).");
        }
        if (r.endDate().isBefore(r.startDate())) {
            throw new InvalidRentalException("A bérlés vége nem lehet korábbi a kezdeténél.");
        }
        long days = ChronoUnit.DAYS.between(r.startDate(), r.endDate()) + 1;
        if (days < MIN_DAYS) {
            throw new InvalidRentalException("A bérlésnek legalább " + MIN_DAYS
                    + " naposnak kell lennie (a kezdő- és a záródátum is beleszámít); a megadott időszak "
                    + days + " napos.");
        }
        if (days > MAX_DAYS) {
            throw new InvalidRentalException("A bérlés legfeljebb " + MAX_DAYS
                    + " napos lehet (a kezdő- és a záródátum is beleszámít); a megadott időszak " + days + " napos.");
        }
        if (repository.existsByChefIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                r.chefId(), r.endDate(), r.startDate())) {
            throw new InvalidRentalException("A séf (chefId: " + r.chefId()
                    + ") a megadott időszakban már foglalt: a bérlések nem fedhetik át egymást.");
        }
    }
}
