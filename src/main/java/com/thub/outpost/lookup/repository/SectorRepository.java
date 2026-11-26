package com.thub.outpost.lookup.repository;

import com.thub.outpost.lookup.entity.Sector;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, UUID> {
}
