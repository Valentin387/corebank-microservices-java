package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Balance entities.
 */
@Repository
public interface BalanceJpaRepository extends JpaRepository<Balance, Long> {

    Optional<Balance> findByCustomerId(String customerId);
}
