package com.thub.outpost.grant.controller;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import com.thub.outpost.support.TestDataCleaner;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * OP-012: keyword search (q), page size cap and sort whitelist on the founder listing,
 * plus the same paging rules on the admin list.
 */
class GrantSearchAndPagingIT extends AbstractIntegrationTest {

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
    Clock clock;

    LocalDate today;

    @BeforeEach
    void setUp() {
        cleaner.clean("grant");
        today = LocalDate.now(clock);
    }

    // ---- keyword search --------------------------------------------------------------------------

    @Test
    void q_matchesTheName_ignoringCase() throws Exception {
        grants.save(aGrant().named("Women Entrepreneurship Scheme").published().build());
        grants.save(aGrant().named("Seed Fund").published().build());

        listing("q", "women").andExpect(jsonPath("$.content[*].name", contains("Women Entrepreneurship Scheme")));
    }

    @Test
    void q_matchesTheIssuingBody() throws Exception {
        grants.save(aGrant().named("Scheme A").published().issuedBy("Ministry of MSME", IssuingBodyType.CENTRAL).build());
        grants.save(aGrant().named("Scheme B").published().issuedBy("DPIIT", IssuingBodyType.CENTRAL).build());

        listing("q", "msme").andExpect(jsonPath("$.content[*].name", contains("Scheme A")));
    }

    @Test
    void q_matchesTheSummary_andPartialWords() throws Exception {
        grants.save(aGrant().named("Scheme A").published().withSummary("Support for women entrepreneurs in rural areas").build());
        grants.save(aGrant().named("Scheme B").published().withSummary("Funding for deep tech").build());

        listing("q", "ENTREPRENEUR").andExpect(jsonPath("$.content[*].name", contains("Scheme A")));
    }

    @Test
    void q_doesNotMatchOtherFields() throws Exception {
        // "eligibility" is not one of the three searched fields
        grants.save(aGrant().named("Scheme A").published().withEligibility("Only for women-led startups").build());

        listing("q", "women").andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void q_combinesWithTheOtherFilters() throws Exception {
        grants.save(aGrant().named("Women scheme").published().ofType(GrantType.SCHEME).build());
        grants.save(aGrant().named("Women grant").published().ofType(GrantType.GRANT).build());
        grants.save(aGrant().named("Other scheme").published().ofType(GrantType.SCHEME).build());

        listing("q", "women", "type", "SCHEME").andExpect(jsonPath("$.content[*].name", contains("Women scheme")));
    }

    @Test
    void q_neverRevealsDraftsOrExpiredGrants() throws Exception {
        grants.save(aGrant().named("Women draft").build());
        grants.save(aGrant().named("Women expired").published().expired().build());

        listing("q", "women").andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void q_treatsPercentAndUnderscoreAsPlainCharacters() throws Exception {
        grants.save(aGrant().named("100% subsidy").published().build());
        grants.save(aGrant().named("1000 grant").published().build());
        grants.save(aGrant().named("Tech_Grant").published().build());
        grants.save(aGrant().named("TechXGrant").published().build());

        // Unescaped, "100%" would also match "1000 grant" and "%" would match everything.
        listing("q", "100%").andExpect(jsonPath("$.content[*].name", contains("100% subsidy")));
        listing("q", "%").andExpect(jsonPath("$.content[*].name", contains("100% subsidy")));
        listing("q", "tech_").andExpect(jsonPath("$.content[*].name", contains("Tech_Grant")));
    }

    @Test
    void blankQ_isIgnored() throws Exception {
        grants.save(aGrant().named("A").published().build());
        grants.save(aGrant().named("B").published().build());

        listing("q", "   ").andExpect(jsonPath("$.totalElements").value(2));
    }

    // ---- page size cap ---------------------------------------------------------------------------

    @Test
    void sizeAbove50_returns400WithAFieldError() throws Exception {
        listing("size", "51").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void sizeOf50_isAllowed() throws Exception {
        listing("size", "50").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

    // ---- sort whitelist --------------------------------------------------------------------------

    @Test
    void sortByAFieldNotOnTheWhitelist_returns400() throws Exception {
        listing("sort", "evil").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
        // Real attributes that are not whitelisted are rejected too, as are relation paths.
        listing("sort", "summary").andExpect(status().isBadRequest());
        listing("sort", "stages.name").andExpect(status().isBadRequest());
    }

    @Test
    void sortByName_bothDirections() throws Exception {
        grants.save(aGrant().named("Beta").published().build());
        grants.save(aGrant().named("Alpha").published().build());
        grants.save(aGrant().named("Gamma").published().build());

        listing("sort", "name,asc").andExpect(jsonPath("$.content[*].name", contains("Alpha", "Beta", "Gamma")));
        listing("sort", "name,desc").andExpect(jsonPath("$.content[*].name", contains("Gamma", "Beta", "Alpha")));
    }

    @Test
    void rollingGrantsSortLast_evenWhenSortingByDeadlineDescending() throws Exception {
        grants.save(aGrant().named("Rolling").published().rolling().build());
        grants.save(aGrant().named("Soon").published().withDeadline(today.plusDays(5)).build());
        grants.save(aGrant().named("Later").published().withDeadline(today.plusDays(50)).build());

        listing("sort", "deadline,asc").andExpect(jsonPath("$.content[*].name", contains("Soon", "Later", "Rolling")));
        listing("sort", "deadline,desc").andExpect(jsonPath("$.content[*].name", contains("Later", "Soon", "Rolling")));
    }

    @Test
    void sortByCreatedAt_isAllowed() throws Exception {
        listing("sort", "createdAt,desc").andExpect(status().isOk());
    }

    // ---- pagination ------------------------------------------------------------------------------

    @Test
    void page2HasDifferentGrantsThanPage1() throws Exception {
        for (int i = 1; i <= 5; i++) {
            grants.save(aGrant().named("Grant " + i).published().build());
        }

        List<String> page1 = names(listing("size", "2", "page", "0"));
        List<String> page2 = names(listing("size", "2", "page", "1"));

        org.assertj.core.api.Assertions.assertThat(page1).hasSize(2).doesNotContainAnyElementsOf(page2);
        org.assertj.core.api.Assertions.assertThat(page2).hasSize(2);
    }

    @Test
    void pagingFollowsTheDefaultOrder_withRollingGrantsOnTheLastPage() throws Exception {
        grants.save(aGrant().named("Rolling B").published().rolling().build());
        grants.save(aGrant().named("Dated 1").published().withDeadline(today.plusDays(1)).build());
        grants.save(aGrant().named("Rolling A").published().rolling().build());
        grants.save(aGrant().named("Dated 2").published().withDeadline(today.plusDays(2)).build());

        listing("size", "2", "page", "0").andExpect(jsonPath("$.content[*].name", contains("Dated 1", "Dated 2")));
        listing("size", "2", "page", "1").andExpect(jsonPath("$.content[*].name", contains("Rolling A", "Rolling B")));
    }

    @Test
    void pageEnvelopeFollowsTheConvention() throws Exception {
        for (int i = 1; i <= 3; i++) {
            grants.save(aGrant().named("Grant " + i).published().build());
        }

        listing("size", "2", "page", "1")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // ---- the same rules on the admin list --------------------------------------------------------

    @Test
    void adminList_appliesTheSameSizeCapAndSortWhitelist() throws Exception {
        grants.save(aGrant().named("Beta").build());
        grants.save(aGrant().named("Alpha").published().build());

        mockMvc.perform(get("/api/v1/admin/grants").param("size", "51")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/grants").param("sort", "evil")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/grants").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Alpha", "Beta")))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private ResultActions listing(String... keyValues) throws Exception {
        var request = get(URL).with(user("founder"));
        for (int i = 0; i < keyValues.length; i += 2) {
            request = request.param(keyValues[i], keyValues[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private List<String> names(ResultActions result) throws Exception {
        JsonNode body = json.readTree(result.andReturn().getResponse().getContentAsString());
        List<String> names = new ArrayList<>();
        body.get("content").forEach(card -> names.add(card.get("name").asText()));
        return names;
    }
}
