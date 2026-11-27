package com.thub.outpost.support;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Test Data Builder for {@link Grant}: sensible defaults, and each test overrides only what it cares about.
 *
 * <pre>
 * Grant g = GrantBuilder.aGrant().published().withDeadline(LocalDate.of(2026, 12, 31)).build();
 * </pre>
 *
 * build() returns an unsaved entity; save it with the repository. Add methods here as tests need them.
 */
public final class GrantBuilder {

    private String name = "Startup India Seed Fund";
    private String issuingBody = "DPIIT";
    private IssuingBodyType issuingBodyType = IssuingBodyType.CENTRAL;
    private GrantType type = GrantType.GRANT;
    private String summary = "Seed funding for early-stage startups.";
    private String sourceUrl = "https://example.org/seed-fund";
    private String eligibility;
    private LocalDate deadline;
    private BigDecimal amountMax;
    private String amountDisplay;
    private boolean published;
    private boolean expired;
    private Stage[] stages = new Stage[0];
    private Sector[] sectors = new Sector[0];

    private GrantBuilder() {
    }

    public static GrantBuilder aGrant() {
        return new GrantBuilder();
    }

    public GrantBuilder named(String name) {
        this.name = name;
        return this;
    }

    public GrantBuilder issuedBy(String issuingBody, IssuingBodyType issuingBodyType) {
        this.issuingBody = issuingBody;
        this.issuingBodyType = issuingBodyType;
        return this;
    }

    public GrantBuilder ofType(GrantType type) {
        this.type = type;
        return this;
    }

    public GrantBuilder withSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public GrantBuilder withEligibility(String eligibility) {
        this.eligibility = eligibility;
        return this;
    }

    public GrantBuilder withDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }

    /** The most a founder can receive; this is what the amount bands are based on. */
    public GrantBuilder withAmountMax(long amountMax) {
        this.amountMax = BigDecimal.valueOf(amountMax);
        return this;
    }

    public GrantBuilder withAmountDisplay(String amountDisplay) {
        this.amountDisplay = amountDisplay;
        return this;
    }

    /** No deadline: a rolling grant. This is also the default. */
    public GrantBuilder rolling() {
        this.deadline = null;
        return this;
    }

    public GrantBuilder published() {
        this.published = true;
        return this;
    }

    public GrantBuilder expired() {
        this.expired = true;
        return this;
    }

    public GrantBuilder withStages(Stage... stages) {
        this.stages = stages;
        return this;
    }

    public GrantBuilder withSectors(Sector... sectors) {
        this.sectors = sectors;
        return this;
    }

    public Grant build() {
        Grant grant = new Grant(name, issuingBody, issuingBodyType, type, summary, sourceUrl);
        grant.setDeadline(deadline);
        grant.setEligibility(eligibility);
        grant.setAmountMax(amountMax);
        grant.setAmountDisplay(amountDisplay);
        grant.setPublished(published);
        grant.setExpired(expired);
        for (Stage stage : stages) {
            grant.addStage(stage);
        }
        for (Sector sector : sectors) {
            grant.addSector(sector);
        }
        return grant;
    }
}
