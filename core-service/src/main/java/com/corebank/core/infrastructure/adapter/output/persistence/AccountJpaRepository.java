package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Account entities.
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    List<Account> findByCustomerId(String customerId);
}
