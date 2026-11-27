package com.thub.outpost.grant.service;

import com.thub.outpost.common.PageResponse;
import com.thub.outpost.common.error.FieldIssue;
import com.thub.outpost.common.error.NotFoundException;
import com.thub.outpost.common.error.ValidationException;
import com.thub.outpost.grant.dto.GrantRequest;
import com.thub.outpost.grant.dto.GrantResponse;
import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantStatus;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import com.thub.outpost.lookup.repository.SectorRepository;
import com.thub.outpost.lookup.repository.StageRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the admin side of grants: create, edit, list, publish, unpublish, expire, delete.
 *
 * {@code @Transactional} makes each public method run in one database transaction: either every change in
 * the method is saved, or (if an exception is thrown) none of them are. The transaction also keeps the
 * Hibernate session open, so lazy stages and sectors can be read while building the response.
 * Read-only methods say so, which lets Hibernate skip change tracking.
 */
@Service
@Transactional
public class GrantAdminService {

    private final GrantRepository grants;
    private final StageRepository stages;
    private final SectorRepository sectors;

    // Constructor injection: Spring sees the single constructor and passes in the beans it needs.
    public GrantAdminService(GrantRepository grants, StageRepository stages, SectorRepository sectors) {
        this.grants = grants;
        this.stages = stages;
        this.sectors = sectors;
    }

    /** New grants are always drafts (published = false). */
    public GrantResponse create(GrantRequest request) {
        Grant grant = new Grant(request.name(), request.issuingBody(), request.issuingBodyType(), request.type(),
                request.summary(), request.sourceUrl());
        apply(grant, request);
        return GrantResponse.from(grants.saveAndFlush(grant));
    }

    @Transactional(readOnly = true)
    public GrantResponse get(UUID id) {
        return GrantResponse.from(find(id));
    }

    /** @param status optional filter; null returns everything (drafts, published and expired). */
    @Transactional(readOnly = true)
    public PageResponse<GrantResponse> list(GrantStatus status, Pageable requested) {
        Pageable pageable = GrantPaging.validated(requested); // 400 for size > 50 or a sort field not whitelisted
        Page<Grant> page;
        if (status == null) {
            page = grants.findAll(pageable);
        } else {
            page = switch (status) {
                case EXPIRED -> grants.findByExpiredTrue(pageable);
                case PUBLISHED -> grants.findByPublishedTrueAndExpiredFalse(pageable);
                case DRAFT -> grants.findByPublishedFalseAndExpiredFalse(pageable);
            };
        }
        return PageResponse.from(page, GrantResponse::from);
    }

    /**
     * Full replace (PUT): every content field is overwritten with the request values, including the
     * optional ones (omitted means cleared). The published/expired flags are NOT touched here; they only
     * change through publish, unpublish and expire.
     */
    public GrantResponse update(UUID id, GrantRequest request) {
        Grant grant = find(id);
        grant.setName(request.name());
        grant.setIssuingBody(request.issuingBody());
        grant.setIssuingBodyType(request.issuingBodyType());
        grant.setType(request.type());
        grant.setSummary(request.summary());
        grant.setSourceUrl(request.sourceUrl());
        apply(grant, request);
        return GrantResponse.from(grants.saveAndFlush(grant));
    }

    public GrantResponse publish(UUID id) {
        Grant grant = find(id);
        grant.publish();
        return GrantResponse.from(grants.saveAndFlush(grant));
    }

    public GrantResponse unpublish(UUID id) {
        Grant grant = find(id);
        grant.unpublish();
        return GrantResponse.from(grants.saveAndFlush(grant));
    }

    public GrantResponse expire(UUID id) {
        Grant grant = find(id);
        grant.expire();
        return GrantResponse.from(grants.saveAndFlush(grant));
    }

    public void delete(UUID id) {
        grants.delete(find(id));
    }

    // ---------------------------------------------------------------------------------------------

    private Grant find(UUID id) {
        return grants.findById(id).orElseThrow(() -> new NotFoundException("Grant", id));
    }

    /** Copies the optional fields and the stage/sector links from the request onto the grant. */
    private void apply(Grant grant, GrantRequest request) {
        List<FieldIssue> problems = new ArrayList<>();
        if (request.amountMin() != null && request.amountMax() != null
                && request.amountMin().compareTo(request.amountMax()) > 0) {
            problems.add(new FieldIssue("amountMax", "must be greater than or equal to amountMin"));
        }
        Set<Stage> foundStages = resolve(request.stageIds(), "stageIds", "stage", stages::findAllById,
                Stage::getId, problems);
        Set<Sector> foundSectors = resolve(request.sectorIds(), "sectorIds", "sector", sectors::findAllById,
                Sector::getId, problems);
        if (!problems.isEmpty()) {
            throw new ValidationException("VALIDATION_FAILED", "Validation failed", problems);
        }

        grant.setAmountMin(request.amountMin());
        grant.setAmountMax(request.amountMax());
        grant.setAmountDisplay(request.amountDisplay());
        grant.setDeadline(request.deadline());
        grant.setEligibility(request.eligibility());
        grant.setWhatIsFunded(request.whatIsFunded());
        grant.setHowToApply(request.howToApply());

        // Change the existing sets in place (clear + addAll) rather than replacing them, so Hibernate
        // can see exactly which join rows to delete and insert.
        grant.getStages().clear();
        grant.getStages().addAll(foundStages);
        grant.getSectors().clear();
        grant.getSectors().addAll(foundSectors);
    }

    /** Loads the entities for the given ids; any id with no matching row becomes a field error. */
    private static <T> Set<T> resolve(List<UUID> ids, String field, String label,
            Function<Set<UUID>, List<T>> loader, Function<T, UUID> idOf, List<FieldIssue> problems) {
        Set<UUID> wanted = new HashSet<>(ids);
        if (wanted.isEmpty()) {
            return Set.of();
        }
        List<T> found = loader.apply(wanted);
        Set<UUID> foundIds = new HashSet<>();
        found.forEach(entity -> foundIds.add(idOf.apply(entity)));
        wanted.removeAll(foundIds);
        if (!wanted.isEmpty()) {
            problems.add(new FieldIssue(field, "unknown " + label + " id(s): " + wanted));
        }
        return new HashSet<>(found);
    }
}
