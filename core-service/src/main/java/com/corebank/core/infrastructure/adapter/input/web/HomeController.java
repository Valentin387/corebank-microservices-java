package com.corebank.core.infrastructure.adapter.input.web;

import com.corebank.commons.dto.*;
import com.corebank.commons.model.ResponseDTO;
import com.corebank.commons.security.HeaderConstants;
import com.corebank.commons.security.JwtUtil;
import com.corebank.core.application.port.input.GetHomeBalanceUseCase;
import com.corebank.core.domain.model.HomeAggregate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Reactive web adapter (input) for the home/balance endpoint.
 * Preserves the identical API contract from Phase 1.
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final GetHomeBalanceUseCase getHomeBalanceUseCase;
    private final JwtUtil jwtUtil;

    public HomeController(GetHomeBalanceUseCase getHomeBalanceUseCase, JwtUtil jwtUtil) {
        this.getHomeBalanceUseCase = getHomeBalanceUseCase;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/balance")
    public Mono<ResponseDTO<HomeAggregateDTO>> getBalance(ServerHttpRequest request) {
        String customerId = extractCustomerId(request);

        return getHomeBalanceUseCase.getAggregatedBalance(customerId)
                .map(this::toDTO)
                .map(ResponseDTO::success);
    }

    private String extractCustomerId(ServerHttpRequest request) {
        // First try the banking header
        String custIdentNum = request.getHeaders().getFirst(HeaderConstants.X_CUST_IDENT_NUM);
        if (custIdentNum != null) {
            return custIdentNum;
        }

        // Fallback: extract from JWT claims
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(HeaderConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(HeaderConstants.BEARER_PREFIX.length());
            var claims = jwtUtil.extractAllClaims(token);
            Object claimValue = claims.get("custIdentNum");
            if (claimValue != null) {
                return claimValue.toString();
            }
        }

        return "123456789"; // Default for demo
    }

    private HomeAggregateDTO toDTO(HomeAggregate aggregate) {
        return HomeAggregateDTO.builder()
                .accounts(aggregate.getAccounts() != null
                        ? aggregate.getAccounts().stream()
                        .map(a -> AccountDTO.builder()
                                .id(a.getId())
                                .accountNumber(a.getAccountNumber())
                                .accountType(a.getAccountType())
                                .balance(a.getBalance())
                                .build())
                        .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .cards(aggregate.getCards() != null
                        ? aggregate.getCards().stream()
                        .map(c -> CardDTO.builder()
                                .id(c.getId())
                                .cardNumber(c.getCardNumber())
                                .cardType(c.getCardType())
                                .creditLimit(c.getCreditLimit())
                                .availableBalance(c.getAvailableBalance())
                                .build())
                        .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .balance(aggregate.getBalance() != null
                        ? BalanceDTO.builder()
                        .customerId(aggregate.getBalance().getCustomerId())
                        .totalBalance(aggregate.getBalance().getTotalBalance())
                        .availableBalance(aggregate.getBalance().getAvailableBalance())
                        .build()
                        : null)
                .build();
    }
}
