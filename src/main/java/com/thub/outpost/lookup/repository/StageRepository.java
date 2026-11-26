package com.thub.outpost.lookup.repository;

import com.thub.outpost.lookup.entity.Stage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<Stage, UUID> {
}
