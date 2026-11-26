package com.thub.outpost.grant.controller;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import com.thub.outpost.lookup.repository.SectorRepository;
import com.thub.outpost.lookup.repository.StageRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import com.thub.outpost.support.TestDataCleaner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end tests of the admin grants API: real HTTP layer (MockMvc), real service, real Postgres.
 * Not @Transactional on purpose: each request commits for real, like in production.
 * The table is emptied before every test instead.
 */
class AdminGrantControllerIT extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/admin/grants";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    TestDataCleaner cleaner;
    @Autowired
    GrantRepository grants;
    @Autowired
    StageRepository stages;
    @Autowired
    SectorRepository sectors;

    @BeforeEach
    void cleanTables() {
        cleaner.clean("grant"); // CASCADE also empties grant_stage and grant_sector
    }

    // ---- create ----------------------------------------------------------------------------------

    @Test
    void create_returns201WithLocation_asADraftWithItsStagesAndSectors() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Startup India Seed Fund"))
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.stages", hasSize(2)))
                .andExpect(jsonPath("$.sectors", hasSize(2)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
        assertThat(result.getResponse().getHeader("Location")).endsWith(BASE + "/" + id);

        // The Location header really points at the new grant.
        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void create_withInvalidBody_returns400WithAnErrorPerField() throws Exception {
        Map<String, Object> body = validBody();
        body.put("name", "");
        body.put("sourceUrl", "ftp://not-allowed");
        body.remove("type");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'sourceUrl')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'type')]").isNotEmpty());

        assertThat(grants.count()).isZero();
    }

    @Test
    void create_withUnknownStageId_returns400WithAFieldError() throws Exception {
        Map<String, Object> body = validBody();
        body.put("stageIds", List.of(UUID.randomUUID().toString()));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'stageIds')]").isNotEmpty());

        assertThat(grants.count()).isZero();
    }

    @Test
    void create_withUnknownSectorId_returns400WithAFieldError() throws Exception {
        Map<String, Object> body = validBody();
        body.put("sectorIds", List.of(UUID.randomUUID().toString()));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'sectorIds')]").isNotEmpty());
    }

    @Test
    void create_withMinAboveMax_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("amountMin", 500000);
        body.put("amountMax", 100000);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'amountMax')]").isNotEmpty());
    }

    // ---- get -------------------------------------------------------------------------------------

    @Test
    void get_unknownId_returns404WithGrantNotFoundCode() throws Exception {
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"));
    }

    @Test
    void get_withMalformedId_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    // ---- update ----------------------------------------------------------------------------------

    @Test
    void update_replacesTheContent_butKeepsThePublishedFlag() throws Exception {
        String id = createGrant(validBody());
        mockMvc.perform(post(BASE + "/" + id + "/publish")).andExpect(status().isOk());

        Map<String, Object> replacement = validBody();
        replacement.put("name", "Renamed Fund");
        replacement.put("stageIds", List.of()); // full replace: no stages now
        replacement.remove("eligibility");

        mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(replacement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Fund"))
                .andExpect(jsonPath("$.stages", hasSize(0)))
                .andExpect(jsonPath("$.sectors", hasSize(2)))
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void update_unknownId_returns404() throws Exception {
        mockMvc.perform(put(BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"));
    }

    @Test
    void update_withInvalidBody_returns400() throws Exception {
        String id = createGrant(validBody());
        Map<String, Object> body = validBody();
        body.put("summary", " ");

        mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'summary')]").isNotEmpty());
    }

    // ---- publish / unpublish / expire ------------------------------------------------------------

    @Test
    void publish_unpublish_expire_moveTheGrantThroughItsStatuses() throws Exception {
        String id = createGrant(validBody());

        mockMvc.perform(post(BASE + "/" + id + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post(BASE + "/" + id + "/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post(BASE + "/" + id + "/expire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        // The change was really saved, not just echoed back.
        mockMvc.perform(get(BASE + "/" + id)).andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void publish_unknownId_returns404() throws Exception {
        mockMvc.perform(post(BASE + "/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"));
    }

    // ---- list ------------------------------------------------------------------------------------

    @Test
    void list_showsEverythingAndFiltersByStatus() throws Exception {
        grants.save(aGrant().named("A draft").build());
        grants.save(aGrant().named("A published one").published().build());
        grants.save(aGrant().named("An expired one").published().expired().build());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get(BASE).param("status", "published"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A published one"));

        mockMvc.perform(get(BASE).param("status", "draft"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A draft"));

        // An expired grant is EXPIRED even though its published flag is still true.
        mockMvc.perform(get(BASE).param("status", "expired"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("An expired one"));
    }

    @Test
    void list_withUnknownStatus_returns400() throws Exception {
        mockMvc.perform(get(BASE).param("status", "bogus")).andExpect(status().isBadRequest());
    }

    @Test
    void list_isPaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            grants.save(aGrant().named("Grant " + i).build());
        }

        mockMvc.perform(get(BASE).param("size", "2").param("page", "0").param("sort", "name,asc"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Grant 1"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(BASE).param("size", "2").param("page", "1").param("sort", "name,asc"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Grant 3"));
    }

    @Test
    void list_whenEmpty_returns200WithAnEmptyPage() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- delete ----------------------------------------------------------------------------------

    @Test
    void delete_removesTheGrantAndItsLinks() throws Exception {
        String id = createGrant(validBody());

        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
        assertThat(grants.count()).isZero();
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** A valid create/replace body referencing two real (seeded) stages and sectors. */
    private Map<String, Object> validBody() {
        List<String> stageIds = stages.findAll().stream().limit(2).map(Stage::getId).map(UUID::toString).toList();
        List<String> sectorIds = sectors.findAll().stream().limit(2).map(Sector::getId).map(UUID::toString).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Startup India Seed Fund");
        body.put("issuingBody", "DPIIT");
        body.put("issuingBodyType", "CENTRAL");
        body.put("type", "GRANT");
        body.put("summary", "Seed funding for early-stage startups.");
        body.put("eligibility", "DPIIT-recognised startups");
        body.put("sourceUrl", "https://example.org/seed-fund");
        body.put("stageIds", stageIds);
        body.put("sectorIds", sectorIds);
        return body;
    }

    /** Creates a grant through the API and returns its id. */
    private String createGrant(Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = json.readTree(result.getResponse().getContentAsString());
        return created.get("id").asText();
    }
}
