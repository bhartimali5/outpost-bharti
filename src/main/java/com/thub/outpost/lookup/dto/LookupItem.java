package com.thub.outpost.lookup.dto;

import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A stage or sector as it appears inside other responses: just its id and display name. */
public record LookupItem(UUID id, String name) {

    public static LookupItem from(Stage stage) {
        return new LookupItem(stage.getId(), stage.getName());
    }

    public static LookupItem from(Sector sector) {
        return new LookupItem(sector.getId(), sector.getName());
    }

    /** A Set has no order, so sort (dropdown order, then name) for a stable, predictable response. */
    public static List<LookupItem> ofStages(Collection<Stage> stages) {
        return stages.stream()
                .sorted(Comparator.comparingInt(Stage::getSortOrder).thenComparing(Stage::getName))
                .map(LookupItem::from)
                .toList();
    }

    public static List<LookupItem> ofSectors(Collection<Sector> sectors) {
        return sectors.stream()
                .sorted(Comparator.comparingInt(Sector::getSortOrder).thenComparing(Sector::getName))
                .map(LookupItem::from)
                .toList();
    }
}
