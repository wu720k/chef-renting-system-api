package hu.chefrenting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** End-to-end tests (MockMvc + in-memory H2 + seed CSV). Each test is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BerlesApiTest {
    private static final int CHEF = 9001;
    private static final int OTHER_CHEF = 9002;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private static LocalDate in(int days) {
        return LocalDate.now().plusDays(days);
    }

    private ResultActions postRental(int chefId, LocalDate start, LocalDate end) throws Exception {
        return postJson("""
                {"uid": 7, "chefId": %d, "startDate": "%s", "endDate": "%s", "dailyRate": 300, "baseFee": 50}
                """.formatted(chefId, start, end));
    }

    private ResultActions postJson(String json) throws Exception {
        return mvc.perform(post("/api/berlesek").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int createRental(int chefId, LocalDate start, LocalDate end) throws Exception {
        String body = postRental(chefId, start, end).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asInt();
    }

    // ---- GET ----

    @Test
    void getAllReturnsSeedDataWithTotalPrice() throws Exception {
        mvc.perform(get("/api/berlesek"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(300))
                .andExpect(jsonPath("$[0].totalPrice").isNumber());
    }

    @Test
    void getExistingRental() throws Exception {
        // seed row 1: 2025-01-02 .. 2025-01-07 (6 days), dailyRate 165, baseFee 85
        mvc.perform(get("/api/berlesek/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.uid").value(20))
                .andExpect(jsonPath("$.chefId").value(12))
                .andExpect(jsonPath("$.startDate").value("2025-01-02"))
                .andExpect(jsonPath("$.endDate").value("2025-01-07"))
                .andExpect(jsonPath("$.dailyRate").value(165))
                .andExpect(jsonPath("$.baseFee").value(85))
                .andExpect(jsonPath("$.totalPrice").value(85 + 6 * 165));
    }

    @Test
    void getNonexistentRentalIs404() throws Exception {
        mvc.perform(get("/api/berlesek/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("987654")));
    }

    @Test
    void nonNumericIdIs400() throws Exception {
        mvc.perform(get("/api/berlesek/abc")).andExpect(status().isBadRequest());
    }

    // ---- POST: success and price ----

    @Test
    void successfulPostCalculatesTotalPriceAndGeneratesId() throws Exception {
        // 5 days: 50 + 5 * 300 = 1550; client-supplied id and totalPrice must be ignored
        postJson("""
                {"id": 999999, "uid": 7, "chefId": %d, "startDate": "%s", "endDate": "%s",
                 "dailyRate": 300, "baseFee": 50, "totalPrice": 1}
                """.formatted(CHEF, in(10), in(14)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999999)))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(300)))
                .andExpect(jsonPath("$.totalPrice").value(1550))
                .andExpect(jsonPath("$.startDate").value(in(10).toString()));
    }

    @Test
    void postedRentalCanBeReadBack() throws Exception {
        int id = createRental(CHEF, in(10), in(12));
        mvc.perform(get("/api/berlesek/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(50 + 3 * 300));
    }

    @Test
    void dateTimeInputIsAccepted() throws Exception {
        postJson("""
                {"uid": 7, "chefId": %d, "startDate": "%sT00:00:00", "endDate": "%sT00:00:00",
                 "dailyRate": 100, "baseFee": 0}
                """.formatted(CHEF, in(10), in(12)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(300));
    }

    // ---- POST: start date ----

    @Test
    void startingTodayIsRejected() throws Exception {
        postRental(CHEF, in(0), in(5)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("holnap")));
    }

    @Test
    void startingInThePastIsRejected() throws Exception {
        postRental(CHEF, in(-5), in(2)).andExpect(status().isBadRequest());
    }

    @Test
    void startingTomorrowIsAllowed() throws Exception {
        postRental(CHEF, in(1), in(3)).andExpect(status().isCreated());
    }

    // ---- POST: duration ----

    @Test
    void twoDaysIsTooShort() throws Exception {
        postRental(CHEF, in(10), in(11)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("legalább 3")));
    }

    @Test
    void exactlyThreeDaysIsAllowed() throws Exception {
        postRental(CHEF, in(10), in(12)).andExpect(status().isCreated());
    }

    @Test
    void exactlyFourteenDaysIsAllowed() throws Exception {
        postRental(CHEF, in(10), in(23)).andExpect(status().isCreated());
    }

    @Test
    void fifteenDaysIsTooLong() throws Exception {
        postRental(CHEF, in(10), in(24)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("legfeljebb 14")));
    }

    @Test
    void endBeforeStartIsRejected() throws Exception {
        postRental(CHEF, in(12), in(10)).andExpect(status().isBadRequest());
    }

    // ---- POST: availability ----

    @Test
    void overlappingRentalOfSameChefIsRejected() throws Exception {
        createRental(CHEF, in(20), in(24));
        postRental(CHEF, in(22), in(26)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("foglalt")));
    }

    @Test
    void newRentalContainingExistingOneIsRejected() throws Exception {
        createRental(CHEF, in(20), in(22));
        postRental(CHEF, in(18), in(25)).andExpect(status().isBadRequest());
    }

    @Test
    void startingOnTheLastDayOfExistingRentalIsRejected() throws Exception {
        createRental(CHEF, in(20), in(24));
        postRental(CHEF, in(24), in(28)).andExpect(status().isBadRequest());
    }

    @Test
    void endingOnTheFirstDayOfExistingRentalIsRejected() throws Exception {
        createRental(CHEF, in(20), in(24));
        postRental(CHEF, in(17), in(20)).andExpect(status().isBadRequest());
    }

    @Test
    void adjacentRentalsOfSameChefAreAllowed() throws Exception {
        createRental(CHEF, in(20), in(24));
        postRental(CHEF, in(25), in(28)).andExpect(status().isCreated());
        postRental(CHEF, in(16), in(19)).andExpect(status().isCreated());
    }

    @Test
    void overlappingDatesForDifferentChefAreAllowed() throws Exception {
        createRental(CHEF, in(20), in(24));
        postRental(OTHER_CHEF, in(20), in(24)).andExpect(status().isCreated());
    }

    // ---- POST: malformed data ----

    @Test
    void missingFieldsGiveHungarianValidationMessage() throws Exception {
        postJson("{\"uid\": 7}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("kötelező")))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDateGivesBadRequest() throws Exception {
        postJson("""
                {"uid": 7, "chefId": 1, "startDate": "nem-datum", "endDate": "2030-01-01",
                 "dailyRate": 1, "baseFee": 0}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void brokenJsonGivesBadRequest() throws Exception {
        postJson("{ nem json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void emptyBodyGivesBadRequest() throws Exception {
        mvc.perform(post("/api/berlesek").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- DELETE ----

    @Test
    void deleteExistingRental() throws Exception {
        int id = createRental(CHEF, in(10), in(12));
        mvc.perform(delete("/api/berlesek/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/berlesek/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteNonexistentRentalIs404() throws Exception {
        mvc.perform(delete("/api/berlesek/987654")).andExpect(status().isNotFound());
    }

    // ---- totalPrice is not persisted ----

    @Test
    void totalPriceIsInJsonButNotAColumn() throws Exception {
        mvc.perform(get("/api/berlesek/1")).andExpect(jsonPath("$.totalPrice").exists());

        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BERLESEK'", String.class);
        assertTrue(columns.containsAll(List.of("ID", "UID", "CHEF_ID", "START_DATE", "END_DATE", "DAILY_RATE", "BASE_FEE")),
                "columns: " + columns);
        assertFalse(columns.stream().anyMatch(c -> c.contains("TOTAL")), "columns: " + columns);
        assertEquals(7, columns.size());
    }
}
