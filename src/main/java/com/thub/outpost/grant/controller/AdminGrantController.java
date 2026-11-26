package com.thub.outpost.grant.controller;

import com.thub.outpost.common.OpenApiConfig;
import com.thub.outpost.common.PageResponse;
import com.thub.outpost.grant.dto.GrantRequest;
import com.thub.outpost.grant.dto.GrantResponse;
import com.thub.outpost.grant.entity.GrantStatus;
import com.thub.outpost.grant.service.GrantAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Admin endpoints for managing grants. The controller only handles HTTP: it reads the request, calls the
 * service, and shapes the response. Business rules live in {@link GrantAdminService}.
 *
 * TODO(OP-027): these endpoints are open for now. Add @PreAuthorize("hasRole('ADMIN')") on this class and
 * protect /api/v1/admin/** in the security filter chain when admin login exists.
 */
@RestController
@RequestMapping("/api/v1/admin/grants")
@Tag(name = OpenApiConfig.ADMIN_GRANTS)
public class AdminGrantController {

    private final GrantAdminService service;

    public AdminGrantController(GrantAdminService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a grant", description = "Creates a draft (published = false).")
    public ResponseEntity<GrantResponse> create(@Valid @RequestBody GrantRequest request) {
        GrantResponse created = service.create(request);
        // 201 Created, plus a Location header pointing at the new resource, as REST convention expects.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all grants, including drafts and expired",
            description = "Optional status filter: published, draft or expired. Paged: page, size, sort.")
    public PageResponse<GrantResponse> list(
            @RequestParam(required = false) GrantStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one grant")
    public GrantResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a grant",
            description = "Full replace of the content fields. Does not change published/expired.")
    public GrantResponse update(@PathVariable UUID id, @Valid @RequestBody GrantRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a grant (visible to founders)")
    public GrantResponse publish(@PathVariable UUID id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a grant (back to draft)")
    public GrantResponse unpublish(@PathVariable UUID id) {
        return service.unpublish(id);
    }

    @PostMapping("/{id}/expire")
    @Operation(summary = "Mark a grant as expired")
    public GrantResponse expire(@PathVariable UUID id) {
        return service.expire(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a grant")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
