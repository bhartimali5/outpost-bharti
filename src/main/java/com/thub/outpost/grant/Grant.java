package com.thub.outpost.grant;

import com.thub.outpost.common.Auditable;
import com.thub.outpost.lookup.Sector;
import com.thub.outpost.lookup.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A grant, subsidy, scheme or incentive T-Hub lists for founders (spec p.5-6).
 *
 * Why there is no Lombok {@code @Data} here: it generates equals/hashCode over ALL fields, including the
 * lazy stage/sector collections. That triggers database loads from inside equals/hashCode, breaks when a
 * field changes while the entity sits in a HashSet, and treats two unsaved grants as equal. So equals is
 * based on the id alone and hashCode is constant (see the same reasoning on {@link Stage}).
 *
 * The table name is quoted because "grant" is a reserved word in PostgreSQL.
 */
@Entity
@Table(name = "\"grant\"")
public class Grant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "issuing_body", nullable = false)
    private String issuingBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuing_body_type", nullable = false)
    private IssuingBodyType issuingBodyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrantType type;

    @Column(name = "amount_min")
    private BigDecimal amountMin;

    @Column(name = "amount_max")
    private BigDecimal amountMax;

    @Column(name = "amount_display")
    private String amountDisplay;

    /** null means a rolling grant with no deadline. */
    private LocalDate deadline;

    @Column(nullable = false)
    private String summary;

    private String eligibility;

    @Column(name = "what_is_funded")
    private String whatIsFunded;

    @Column(name = "how_to_apply")
    private String howToApply;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false)
    private boolean expired = false;

    // Set, not List: Hibernate handles removing from a Set with one DELETE; a List without an order
    // column deletes and re-inserts every row of the join table.
    // LAZY (the default for collections): stages/sectors load only when someone asks for them.
    @ManyToMany
    @JoinTable(name = "grant_stage",
            joinColumns = @JoinColumn(name = "grant_id"),
            inverseJoinColumns = @JoinColumn(name = "stage_id"))
    private Set<Stage> stages = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "grant_sector",
            joinColumns = @JoinColumn(name = "grant_id"),
            inverseJoinColumns = @JoinColumn(name = "sector_id"))
    private Set<Sector> sectors = new HashSet<>();

    protected Grant() {
        // required by JPA
    }

    /** Only the fields that must always have a value. Optional ones are set with the setters below. */
    public Grant(String name, String issuingBody, IssuingBodyType issuingBodyType, GrantType type,
            String summary, String sourceUrl) {
        this.name = name;
        this.issuingBody = issuingBody;
        this.issuingBodyType = issuingBodyType;
        this.type = type;
        this.summary = summary;
        this.sourceUrl = sourceUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public void setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
    }

    public IssuingBodyType getIssuingBodyType() {
        return issuingBodyType;
    }

    public void setIssuingBodyType(IssuingBodyType issuingBodyType) {
        this.issuingBodyType = issuingBodyType;
    }

    public GrantType getType() {
        return type;
    }

    public void setType(GrantType type) {
        this.type = type;
    }

    public BigDecimal getAmountMin() {
        return amountMin;
    }

    public void setAmountMin(BigDecimal amountMin) {
        this.amountMin = amountMin;
    }

    public BigDecimal getAmountMax() {
        return amountMax;
    }

    public void setAmountMax(BigDecimal amountMax) {
        this.amountMax = amountMax;
    }

    public String getAmountDisplay() {
        return amountDisplay;
    }

    public void setAmountDisplay(String amountDisplay) {
        this.amountDisplay = amountDisplay;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEligibility() {
        return eligibility;
    }

    public void setEligibility(String eligibility) {
        this.eligibility = eligibility;
    }

    public String getWhatIsFunded() {
        return whatIsFunded;
    }

    public void setWhatIsFunded(String whatIsFunded) {
        this.whatIsFunded = whatIsFunded;
    }

    public String getHowToApply() {
        return howToApply;
    }

    public void setHowToApply(String howToApply) {
        this.howToApply = howToApply;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public Set<Stage> getStages() {
        return stages;
    }

    public Set<Sector> getSectors() {
        return sectors;
    }

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public void addSector(Sector sector) {
        sectors.add(sector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Grant other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Grant.class.hashCode();
    }
}
