package com.thub.outpost.lookup;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<Stage, UUID> {
}
