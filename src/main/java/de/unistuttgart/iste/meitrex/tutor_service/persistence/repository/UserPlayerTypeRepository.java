package de.unistuttgart.iste.meitrex.tutor_service.persistence.repository;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for UserPlayerTypeEntity.
 * Provides database access for user player type information.
 */
@Repository
public interface UserPlayerTypeRepository extends JpaRepository<UserPlayerTypeEntity, UUID> {
}
