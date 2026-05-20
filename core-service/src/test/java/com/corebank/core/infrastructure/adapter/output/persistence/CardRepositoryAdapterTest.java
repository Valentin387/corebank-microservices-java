package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Card;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardRepositoryAdapterTest {

    @Mock
    private CardJpaRepository jpaRepository;

    @InjectMocks
    private CardRepositoryAdapter cardRepositoryAdapter;

    @Test
    @DisplayName("findByCustomerId should return a list of cards when found")
    void findByCustomerIdShouldReturnCards() {
        String customerId = "123456789";
        Card card = Card.builder()
                .id(1L)
                .cardNumber("4532-1234-5678-9012")
                .cardType("CREDIT")
                .creditLimit(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("5000.00"))
                .customerId(customerId)
                .build();
        List<Card> expectedCards = List.of(card);

        when(jpaRepository.findByCustomerId(customerId)).thenReturn(expectedCards);

        Mono<List<Card>> result = cardRepositoryAdapter.findByCustomerId(customerId);

        StepVerifier.create(result)
                .expectNextMatches(cards -> 
                        cards.size() == 1 &&
                        cards.get(0).getCustomerId().equals(customerId) &&
                        cards.get(0).getCardNumber().equals("4532-1234-5678-9012"))
                .verifyComplete();
                
        verify(jpaRepository).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("findByCustomerId should return an empty list when no cards found")
    void findByCustomerIdShouldReturnEmptyList() {
        String customerId = "999999999";

        when(jpaRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        Mono<List<Card>> result = cardRepositoryAdapter.findByCustomerId(customerId);

        StepVerifier.create(result)
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
                
        verify(jpaRepository).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("findByCustomerId should propagate errors from the JPA repository")
    void findByCustomerIdShouldPropagateErrors() {
        String customerId = "error-customer";
        RuntimeException dbError = new RuntimeException("Database down");

        when(jpaRepository.findByCustomerId(customerId)).thenThrow(dbError);

        Mono<List<Card>> result = cardRepositoryAdapter.findByCustomerId(customerId);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database down"))
                .verify();
                
        verify(jpaRepository).findByCustomerId(customerId);
    }
}
