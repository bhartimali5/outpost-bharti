package com.thub.outpost.grant.controller;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import com.thub.outpost.lookup.repository.SectorRepository;
import com.thub.outpost.lookup.repository.StageRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import com.thub.outpost.support.TestDataCleaner;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The founder grants listing (GET /api/v1/grants) against a real Postgres.
 * Requests run as a signed-in user via Spring Security's test support, because real login comes with OP-017.
 */
class GrantListingIT extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/grants";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    TestDataCleaner cleaner;
    @Autowired
    GrantRepository grants;
    @Autowired
    StageRepository stageRepository;
    @Autowired
    SectorRepository sectorRepository;
    @Autowired
    Clock clock;
    @Autowired
    EntityManagerFactory entityManagerFactory;

    Stage stageA;
    Stage stageB;
    Sector sectorA;
    Sector sectorB;
    LocalDate today;

    @BeforeEach
    void setUp() {
        cleaner.clean("grant");
        List<Stage> stages = stageRepository.findAll();
        List<Sector> sectors = sectorRepository.findAll();
        stageA = stages.get(0);
        stageB = stages.get(1);
        sectorA = sectors.get(0);
        sectorB = sectors.get(1);
        today = LocalDate.now(clock);
    }

    // ---- default view ----------------------------------------------------------------------------

    @Test
    void onlyPublishedAndNotExpiredGrantsAreListed() throws Exception {
        grants.save(aGrant().named("Visible").published().build());
        grants.save(aGrant().named("Draft").build());
        grants.save(aGrant().named("Expired").published().expired().build());
        grants.save(aGrant().named("Expired draft").expired().build());

        listing().andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].name", contains("Visible")));
    }

    @Test
    void cardHasTheListedFieldsAndNothingInternal() throws Exception {
        grants.save(aGrant().named("Card").published().withAmountDisplay("Up to Rs 50 lakh")
                .withDeadline(today.plusDays(10)).withStages(stageA, stageB).withSectors(sectorA).build());

        listing().andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.content[0].name").value("Card"))
                .andExpect(jsonPath("$.content[0].issuingBody").value("DPIIT"))
                .andExpect(jsonPath("$.content[0].type").value("GRANT"))
                .andExpect(jsonPath("$.content[0].amountDisplay").value("Up to Rs 50 lakh"))
                .andExpect(jsonPath("$.content[0].deadline").value(today.plusDays(10).toString()))
                .andExpect(jsonPath("$.content[0].rolling").value(false))
                .andExpect(jsonPath("$.content[0].stages", hasSize(2)))
                .andExpect(jsonPath("$.content[0].sectors[0].name").value(sectorA.getName()))
                // internal or heavy fields must not leak into the card
                .andExpect(jsonPath("$.content[0].published").doesNotExist())
                .andExpect(jsonPath("$.content[0].expired").doesNotExist())
                .andExpect(jsonPath("$.content[0].summary").doesNotExist())
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist());
    }

    @Test
    void rollingGrantHasNoDeadlineAndIsMarkedRolling() throws Exception {
        grants.save(aGrant().named("Rolling").published().rolling().build());

        listing().andExpect(jsonPath("$.content[0].deadline").value((Object) null))
                .andExpect(jsonPath("$.content[0].rolling").value(true));
    }

    // ---- one filter at a time --------------------------------------------------------------------

    @Test
    void filterByIssuingBodyType() throws Exception {
        grants.save(aGrant().named("Central").published().issuedBy("DPIIT", IssuingBodyType.CENTRAL).build());
        grants.save(aGrant().named("State").published().issuedBy("Telangana", IssuingBodyType.STATE).build());

        listing("issuingBodyType", "STATE").andExpect(jsonPath("$.content[*].name", contains("State")));
    }

    @Test
    void filterByType() throws Exception {
        grants.save(aGrant().named("A grant").published().ofType(GrantType.GRANT).build());
        grants.save(aGrant().named("A scheme").published().ofType(GrantType.SCHEME).build());

        listing("type", "SCHEME").andExpect(jsonPath("$.content[*].name", contains("A scheme")));
    }

    @Test
    void filterByStage_returnsEachMatchingGrantOnce() throws Exception {
        grants.save(aGrant().named("Both stages").published().withStages(stageA, stageB).build());
        grants.save(aGrant().named("Only A").published().withStages(stageA).build());
        grants.save(aGrant().named("Only B").published().withStages(stageB).build());
        grants.save(aGrant().named("No stage").published().build());

        listing("stageId", stageA.getId().toString())
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Both stages", "Only A")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterBySector_returnsEachMatchingGrantOnce() throws Exception {
        grants.save(aGrant().named("Both sectors").published().withSectors(sectorA, sectorB).build());
        grants.save(aGrant().named("Only B").published().withSectors(sectorB).build());
        grants.save(aGrant().named("Only A").published().withSectors(sectorA).build());

        listing("sectorId", sectorB.getId().toString())
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Both sectors", "Only B")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByAmountBand_usesTheMaximumAmountAndHonoursTheBoundaries() throws Exception {
        grants.save(aGrant().named("5 lakh").published().withAmountMax(500_000).build());
        grants.save(aGrant().named("10 lakh").published().withAmountMax(1_000_000).build());
        grants.save(aGrant().named("49.99 lakh").published().withAmountMax(4_999_999).build());
        grants.save(aGrant().named("50 lakh").published().withAmountMax(5_000_000).build());
        grants.save(aGrant().named("1 crore").published().withAmountMax(10_000_000).build());
        grants.save(aGrant().named("No amount").published().build());

        listing("amountBand", "UNDER_10L").andExpect(jsonPath("$.content[*].name", contains("5 lakh")));
        listing("amountBand", "10L_50L")
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("10 lakh", "49.99 lakh")));
        listing("amountBand", "50L_1CR").andExpect(jsonPath("$.content[*].name", contains("50 lakh")));
        listing("amountBand", "OVER_1CR").andExpect(jsonPath("$.content[*].name", contains("1 crore")));
    }

    @Test
    void filterByDeadline() throws Exception {
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        grants.save(aGrant().named("Passed yesterday").published().withDeadline(today.minusDays(1)).build());
        grants.save(aGrant().named("Due today").published().withDeadline(today).build());
        grants.save(aGrant().named("Due end of month").published().withDeadline(endOfMonth).build());
        grants.save(aGrant().named("Due next month").published().withDeadline(endOfMonth.plusDays(1)).build());
        grants.save(aGrant().named("Rolling").published().rolling().build());

        listing("deadline", "OPEN_NOW").andExpect(jsonPath("$.content[*].name",
                containsInAnyOrder("Due today", "Due end of month", "Due next month", "Rolling")));
        listing("deadline", "CLOSING_THIS_MONTH").andExpect(jsonPath("$.content[*].name",
                containsInAnyOrder("Due today", "Due end of month")));
        listing("deadline", "ROLLING").andExpect(jsonPath("$.content[*].name", contains("Rolling")));
    }

    // ---- combinations ----------------------------------------------------------------------------

    @Test
    void filtersCombineWithAnd() throws Exception {
        grants.save(aGrant().named("Match").published().ofType(GrantType.SCHEME)
                .issuedBy("Telangana", IssuingBodyType.STATE).withStages(stageA).build());
        grants.save(aGrant().named("Wrong type").published().ofType(GrantType.GRANT)
                .issuedBy("Telangana", IssuingBodyType.STATE).withStages(stageA).build());
        grants.save(aGrant().named("Wrong body").published().ofType(GrantType.SCHEME)
                .issuedBy("DPIIT", IssuingBodyType.CENTRAL).withStages(stageA).build());
        grants.save(aGrant().named("Wrong stage").published().ofType(GrantType.SCHEME)
                .issuedBy("Telangana", IssuingBodyType.STATE).withStages(stageB).build());

        listing("type", "SCHEME", "issuingBodyType", "STATE", "stageId", stageA.getId().toString())
                .andExpect(jsonPath("$.content[*].name", contains("Match")));
    }

    @Test
    void stageAndSectorFiltersTogether() throws Exception {
        grants.save(aGrant().named("Match").published().withStages(stageA, stageB).withSectors(sectorA, sectorB).build());
        grants.save(aGrant().named("Stage only").published().withStages(stageA).build());

        listing("stageId", stageA.getId().toString(), "sectorId", sectorB.getId().toString())
                .andExpect(jsonPath("$.content[*].name", contains("Match")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filtersNeverRevealDraftsOrExpiredGrants() throws Exception {
        grants.save(aGrant().named("Draft").ofType(GrantType.SCHEME).withStages(stageA).build());
        grants.save(aGrant().named("Expired").published().expired().ofType(GrantType.SCHEME).withStages(stageA).build());

        listing("type", "SCHEME", "stageId", stageA.getId().toString())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- empty state, order, paging --------------------------------------------------------------

    @Test
    void noMatches_returns200WithAnEmptyPage() throws Exception {
        grants.save(aGrant().named("Something").published().ofType(GrantType.GRANT).build());

        listing("type", "INCENTIVE").andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void defaultOrderIsDeadlineSoonestFirst_thenRollingLast_thenByName() throws Exception {
        grants.save(aGrant().named("Mid (rolling)").published().rolling().build());
        grants.save(aGrant().named("Alpha (later)").published().withDeadline(today.plusDays(30)).build());
        grants.save(aGrant().named("Beta (rolling)").published().rolling().build());
        grants.save(aGrant().named("Zeta (sooner)").published().withDeadline(today.plusDays(10)).build());

        listing().andExpect(jsonPath("$.content[*].name",
                contains("Zeta (sooner)", "Alpha (later)", "Beta (rolling)", "Mid (rolling)")));
    }

    @Test
    void resultsArePaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            grants.save(aGrant().named("Grant " + i).published().build());
        }

        listing("size", "2", "page", "0")
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        listing("size", "2", "page", "1").andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- bad input -------------------------------------------------------------------------------

    @Test
    void unknownFilterValues_return400() throws Exception {
        listing("amountBand", "BOGUS").andExpect(status().isBadRequest());
        listing("type", "BOGUS").andExpect(status().isBadRequest());
        listing("deadline", "BOGUS").andExpect(status().isBadRequest());
        listing("stageId", "not-a-uuid").andExpect(status().isBadRequest());
    }

    // ---- links with the admin side and the number of queries -------------------------------------

    @Test
    void aGrantPublishedByAnAdminAppearsInTheFounderListing() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Published by admin", "issuingBody", "DPIIT", "issuingBodyType", "CENTRAL",
                "type", "GRANT", "summary", "s", "sourceUrl", "https://example.org/x",
                "stageIds", List.of(stageA.getId().toString()));
        String created = mockMvc.perform(post("/api/v1/admin/grants").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();

        listing().andExpect(jsonPath("$.content", hasSize(0))); // still a draft
        mockMvc.perform(post("/api/v1/admin/grants/" + UUID.fromString(id) + "/publish")).andExpect(status().isOk());

        listing().andExpect(jsonPath("$.content[*].name", contains("Published by admin")));
    }

    @Test
    void aPageOfGrantsDoesNotRunOneQueryPerGrant() throws Exception {
        for (int i = 1; i <= 10; i++) {
            grants.save(aGrant().named("Grant " + i).published().withStages(stageA, stageB)
                    .withSectors(sectorA, sectorB).build());
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            listing().andExpect(jsonPath("$.content", hasSize(10)));

            // count query + grants + stages (batched) + sectors (batched). Without batch fetching this would be 22.
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** GET /api/v1/grants as a signed-in founder, with query parameters given as key, value, key, value, ... */
    private ResultActions listing(String... keyValues) throws Exception {
        var request = get(URL).with(user("founder"));
        for (int i = 0; i < keyValues.length; i += 2) {
            request = request.param(keyValues[i], keyValues[i + 1]);
        }
        return mockMvc.perform(request);
    }
}
