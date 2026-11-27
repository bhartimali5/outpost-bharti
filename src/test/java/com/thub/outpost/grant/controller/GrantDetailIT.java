package com.thub.outpost.grant.controller;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import com.thub.outpost.lookup.repository.SectorRepository;
import com.thub.outpost.lookup.repository.StageRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import com.thub.outpost.support.TestDataCleaner;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** OP-014: the founder grant detail page and the empty-state contract, against a real Postgres. */
class GrantDetailIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestDataCleaner cleaner;
    @Autowired
    GrantRepository grants;
    @Autowired
    StageRepository stageRepository;
    @Autowired
    SectorRepository sectorRepository;

    @BeforeEach
    void cleanTables() {
        cleaner.clean("grant");
    }

    // ---- detail ----------------------------------------------------------------------------------

    @Test
    void publishedGrant_returns200WithEverythingTheSpecLists() throws Exception {
        List<Stage> stages = stageRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Stage::getSortOrder)).limit(2).toList();
        Sector sector = sectorRepository.findAll().get(0);
        // Added in reverse dropdown order, to prove the response sorts them.
        UUID id = save(aGrant().named("Women Entrepreneurship Scheme").published()
                .issuedBy("Ministry of MSME", IssuingBodyType.CENTRAL).ofType(GrantType.SCHEME)
                .withAmountDisplay("Up to Rs 50 lakh").withAmountMax(5_000_000)
                .withDeadline(LocalDate.of(2027, 3, 31))
                .withSummary("Support for women-led startups.")
                .withEligibility("At least 51% women ownership.")
                .withWhatIsFunded("Equipment and working capital.")
                .withHowToApply("Apply on the ministry portal.")
                .withSourceUrl("https://msme.gov.in/women-scheme")
                .withStages(stages.get(1), stages.get(0)).withSectors(sector)
                .build());

        detail(id).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Women Entrepreneurship Scheme"))
                .andExpect(jsonPath("$.issuingBody").value("Ministry of MSME"))
                .andExpect(jsonPath("$.type").value("SCHEME"))
                .andExpect(jsonPath("$.amountDisplay").value("Up to Rs 50 lakh"))
                .andExpect(jsonPath("$.deadline").value("2027-03-31"))
                .andExpect(jsonPath("$.rolling").value(false))
                .andExpect(jsonPath("$.summary").value("Support for women-led startups."))
                .andExpect(jsonPath("$.eligibility").value("At least 51% women ownership."))
                .andExpect(jsonPath("$.whatIsFunded").value("Equipment and working capital."))
                .andExpect(jsonPath("$.howToApply").value("Apply on the ministry portal."))
                .andExpect(jsonPath("$.sourceUrl").value("https://msme.gov.in/women-scheme"))
                .andExpect(jsonPath("$.stages", hasSize(2)))
                .andExpect(jsonPath("$.stages[0].name").value(stages.get(0).getName()))
                .andExpect(jsonPath("$.stages[1].name").value(stages.get(1).getName()))
                .andExpect(jsonPath("$.sectors[0].id").value(sector.getId().toString()))
                .andExpect(jsonPath("$.expired").value(false))
                // internal fields stay out of the founder view
                .andExpect(jsonPath("$.published").doesNotExist())
                .andExpect(jsonPath("$.amountMax").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void rollingGrant_hasNoDeadlineAndIsMarkedRolling() throws Exception {
        UUID id = save(aGrant().published().rolling().build());

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value((Object) null))
                .andExpect(jsonPath("$.rolling").value(true));
    }

    @Test
    void expiredGrant_returns200MarkedExpired() throws Exception {
        UUID id = save(aGrant().named("Closed scheme").published().expired().build());

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Closed scheme"))
                .andExpect(jsonPath("$.expired").value(true));
    }

    @Test
    void draft_returns404_exactlyLikeAnUnknownId() throws Exception {
        UUID draft = save(aGrant().named("Secret draft").build());
        UUID unknown = UUID.randomUUID();

        detail(draft).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Grant with id " + draft + " was not found"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Secret draft"))));
        detail(unknown).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Grant with id " + unknown + " was not found"));
    }

    @Test
    void expiredDraft_returns404() throws Exception {
        UUID id = save(aGrant().expired().build());

        detail(id).andExpect(status().isNotFound());
    }

    @Test
    void unknownId_returns404() throws Exception {
        detail(UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/grants/not-a-uuid").with(user("founder")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_requiresASignedInUser() throws Exception {
        UUID id = save(aGrant().published().build());

        mockMvc.perform(get("/api/v1/grants/" + id)).andExpect(status().isForbidden());
    }

    // ---- empty-state contract --------------------------------------------------------------------

    @Test
    void listingWithNoGrantsAtAll_returns200WithAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/grants").with(user("founder")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listingWhereOnlyDraftsExist_returns200WithAnEmptyPage() throws Exception {
        save(aGrant().named("Draft").build());

        mockMvc.perform(get("/api/v1/grants").with(user("founder")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void aPageBeyondTheLastOne_returns200WithEmptyContentAndTheRealTotals() throws Exception {
        save(aGrant().published().build());

        mockMvc.perform(get("/api/v1/grants").param("page", "5").with(user("founder")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void theEmptyStateContractAndThe404AreDocumentedInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/grants'].get.responses['200'].description",
                        containsString("EMPTY-STATE CONTRACT")))
                .andExpect(jsonPath("$.paths['/api/v1/grants'].get.responses['200'].description",
                        containsString("never a 404")))
                .andExpect(jsonPath("$.paths['/api/v1/grants/{id}'].get.responses['404'].description",
                        containsString("GRANT_NOT_FOUND")))
                .andExpect(jsonPath("$.paths['/api/v1/grants/{id}'].get.tags[0]").value("Grants"));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private UUID save(Grant grant) {
        return grants.save(grant).getId();
    }

    private ResultActions detail(UUID id) throws Exception {
        return mockMvc.perform(get("/api/v1/grants/" + id).with(user("founder")));
    }
}
