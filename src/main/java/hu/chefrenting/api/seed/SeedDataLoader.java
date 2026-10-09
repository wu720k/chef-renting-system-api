package hu.chefrenting.api.seed;

import hu.chefrenting.api.repository.BerlesRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the seed rentals (id;uid;chefId;startDate;endDate;dailyRate;baseFee) from the classpath CSV
 * at startup, when the table is empty. The business rules of POST are not applied to seed data.
 * Rows are inserted with their CSV ids; the identity counter is then moved past the highest id.
 */
@Component
public class SeedDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final String HEADER = "id;uid;chefId;startDate;endDate;dailyRate;baseFee";

    private final BerlesRepository repository;
    private final JdbcTemplate jdbc;
    private final String seedFile;

    public SeedDataLoader(BerlesRepository repository, JdbcTemplate jdbc, @Value("${app.seed.file}") String seedFile) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.seedFile = seedFile;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (repository.count() > 0) {
            return;
        }
        List<Object[]> rows = read();
        jdbc.batchUpdate("INSERT INTO berlesek (id, uid, chef_id, start_date, end_date, daily_rate, base_fee) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", rows);
        int maxId = rows.stream().mapToInt(r -> (Integer) r[0]).max().orElse(0);
        jdbc.execute("ALTER TABLE berlesek ALTER COLUMN id RESTART WITH " + (maxId + 1));
        log.info("{} bérlés betöltve a(z) {} fájlból.", rows.size(), seedFile);
    }

    private List<Object[]> read() throws IOException {
        List<Object[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(seedFile).getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.replace("﻿", "").trim().equals(HEADER)) {
                throw new IOException("Váratlan CSV fejléc a(z) " + seedFile + " fájlban, a várt: " + HEADER);
            }
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split(";", -1);
                if (f.length != 7) {
                    throw new IOException(lineNo + ". sor: 7 oszlop helyett " + f.length + " található");
                }
                rows.add(new Object[]{Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()),
                        Integer.parseInt(f[2].trim()), LocalDate.parse(f[3].trim()), LocalDate.parse(f[4].trim()),
                        Integer.parseInt(f[5].trim()), Integer.parseInt(f[6].trim())});
            }
        }
        return rows;
    }
}
