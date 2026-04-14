package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.SuspensionAppeal;
import com.sliitreserve.api.entities.auth.SuspensionAppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<SuspensionAppeal, UUID> {

    List<SuspensionAppeal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SuspensionAppeal> findByStatusOrderByCreatedAtAsc(SuspensionAppealStatus status);

    Page<SuspensionAppeal> findByStatus(SuspensionAppealStatus status, Pageable pageable);

    Optional<SuspensionAppeal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SuspensionAppealStatus status);

    long countByUserIdAndStatus(UUID userId, SuspensionAppealStatus status);
}
