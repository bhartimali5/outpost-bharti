package com.thub.outpost.lookup.dto;

import com.thub.outpost.lookup.entity.Sector;
import com.thub.outpost.lookup.entity.Stage;
import java.util.UUID;

/** A stage or sector as it appears inside other responses: just its id and display name. */
public record LookupItem(UUID id, String name) {

    public static LookupItem from(Stage stage) {
        return new LookupItem(stage.getId(), stage.getName());
    }

    public static LookupItem from(Sector sector) {
        return new LookupItem(sector.getId(), sector.getName());
    }
}
