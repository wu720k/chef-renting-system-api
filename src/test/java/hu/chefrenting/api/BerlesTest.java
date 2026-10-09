package hu.chefrenting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hu.chefrenting.api.entity.Berles;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BerlesTest {

    @Test
    void totalPriceIsBaseFeePlusInclusiveDaysTimesDailyRate() {
        Berles b = new Berles(1, 2, LocalDate.of(2030, 3, 4), LocalDate.of(2030, 3, 6), 300, 50);
        assertEquals(3, b.getDays());
        assertEquals(50 + 3 * 300, b.getTotalPrice());
    }

    @Test
    void singleDayRentalCountsAsOneDay() {
        Berles b = new Berles(1, 2, LocalDate.of(2030, 3, 4), LocalDate.of(2030, 3, 4), 100, 10);
        assertEquals(1, b.getDays());
        assertEquals(110, b.getTotalPrice());
    }

    @Test
    void rentalCrossingMonthBoundary() {
        Berles b = new Berles(1, 2, LocalDate.of(2024, 2, 27), LocalDate.of(2024, 3, 2), 100, 0);
        assertEquals(5, b.getDays()); // 2024 is a leap year: 27, 28, 29, Mar 1, Mar 2
        assertEquals(500, b.getTotalPrice());
    }
}
