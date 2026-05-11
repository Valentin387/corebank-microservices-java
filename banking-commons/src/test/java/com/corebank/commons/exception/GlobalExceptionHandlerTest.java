package com.corebank.commons.exception;

import com.corebank.commons.model.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleIllegalArgument() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

        // Act
        ResponseEntity<ResponseDTO<String>> response = exceptionHandler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatusCode());
        assertEquals("Invalid argument provided", response.getBody().getBody());
    }

    @Test
    void testHandleSecurity() {
        // Arrange
        SecurityException ex = new SecurityException("Unauthorized access");

        // Act
        ResponseEntity<ResponseDTO<String>> response = exceptionHandler.handleSecurity(ex);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatusCode());
        assertEquals("Unauthorized access", response.getBody().getBody());
    }

    @Test
    void testHandleRuntime() {
        // Arrange
        RuntimeException ex = new RuntimeException("Runtime failure");

        // Act
        ResponseEntity<ResponseDTO<String>> response = exceptionHandler.handleRuntime(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatusCode());
        assertEquals("Runtime failure", response.getBody().getBody());
    }

    @Test
    void testHandleGeneral() {
        // Arrange
        Exception ex = new Exception("Some hidden error detail that should not be exposed");

        // Act
        ResponseEntity<ResponseDTO<String>> response = exceptionHandler.handleGeneral(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatusCode());
        // For general Exception, the handler overrides the message with a generic one
        assertEquals("An unexpected error occurred", response.getBody().getBody());
    }
}
