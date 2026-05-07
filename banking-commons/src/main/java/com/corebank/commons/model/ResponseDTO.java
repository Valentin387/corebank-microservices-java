package com.corebank.commons.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standardized response wrapper used across all CoreBank microservices.
 * Preserves the identical contract from Phase 1 monolith.
 *
 * @param <T> the type of the response body
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {
    private int statusCode;
    private T body;
    private Map<String, Object> extraArgs;

    public static <T> ResponseDTO<T> success(T body) {
        return ResponseDTO.<T>builder()
                .statusCode(200)
                .body(body)
                .build();
    }

    public static <T> ResponseDTO<T> error(int statusCode, T body) {
        return ResponseDTO.<T>builder()
                .statusCode(statusCode)
                .body(body)
                .build();
    }

    public static <T> ResponseDTO<T> error(int statusCode, T body, Map<String, Object> extraArgs) {
        return ResponseDTO.<T>builder()
                .statusCode(statusCode)
                .body(body)
                .extraArgs(extraArgs)
                .build();
    }
}
