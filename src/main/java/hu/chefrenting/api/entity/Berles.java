package hu.chefrenting.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One chef rental. totalPrice is calculated and is not a database column. */
@Entity
@Table(name = "berlesek")
public class Berles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int uid;
    private int chefId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int dailyRate;
    private int baseFee;

    protected Berles() {
        // required by JPA
    }

    public Berles(int uid, int chefId, LocalDate startDate, LocalDate endDate, int dailyRate, int baseFee) {
        this.uid = uid;
        this.chefId = chefId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyRate = dailyRate;
        this.baseFee = baseFee;
    }

    /** Number of rental days; both the start and the end date count. */
    @Transient
    @JsonIgnore
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** baseFee + days x dailyRate (not stored, calculated on every read). */
    @Transient
    public int getTotalPrice() {
        return Math.toIntExact(baseFee + getDays() * dailyRate);
    }

    public Integer getId() { return id; }
    public int getUid() { return uid; }
    public int getChefId() { return chefId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDailyRate() { return dailyRate; }
    public int getBaseFee() { return baseFee; }
}
