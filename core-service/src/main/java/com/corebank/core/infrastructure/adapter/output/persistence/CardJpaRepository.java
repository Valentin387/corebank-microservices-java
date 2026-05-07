package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Card entities.
 */
@Repository
public interface CardJpaRepository extends JpaRepository<Card, Long> {

    List<Card> findByCustomerId(String customerId);
}
