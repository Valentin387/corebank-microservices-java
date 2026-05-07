package com.corebank.commons.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseDTOTest {

    @Test
    @DisplayName("success() should create ResponseDTO with status 200")
    void successShouldCreateResponseWith200() {
        ResponseDTO<String> response = ResponseDTO.success("test-body");

        assertEquals(200, response.getStatusCode());
        assertEquals("test-body", response.getBody());
        assertNull(response.getExtraArgs());
    }

    @Test
    @DisplayName("error() should create ResponseDTO with given status code")
    void errorShouldCreateResponseWithGivenStatusCode() {
        ResponseDTO<String> response = ResponseDTO.error(401, "Unauthorized");

        assertEquals(401, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody());
        assertNull(response.getExtraArgs());
    }

    @Test
    @DisplayName("error() with extraArgs should include them in response")
    void errorWithExtraArgsShouldIncludeThem() {
        Map<String, Object> extras = Map.of("field", "username");
        ResponseDTO<String> response = ResponseDTO.error(400, "Validation failed", extras);

        assertEquals(400, response.getStatusCode());
        assertEquals("Validation failed", response.getBody());
        assertNotNull(response.getExtraArgs());
        assertEquals("username", response.getExtraArgs().get("field"));
    }

    @Test
    @DisplayName("Builder should create ResponseDTO with all fields")
    void builderShouldCreateFullResponse() {
        ResponseDTO<Integer> response = ResponseDTO.<Integer>builder()
                .statusCode(200)
                .body(42)
                .extraArgs(Map.of("key", "value"))
                .build();

        assertEquals(200, response.getStatusCode());
        assertEquals(42, response.getBody());
        assertEquals("value", response.getExtraArgs().get("key"));
    }

    @Test
    @DisplayName("NoArgsConstructor should create empty ResponseDTO")
    void noArgsConstructorShouldCreateEmpty() {
        ResponseDTO<String> response = new ResponseDTO<>();

        assertEquals(0, response.getStatusCode());
        assertNull(response.getBody());
        assertNull(response.getExtraArgs());
    }

    @Test
    @DisplayName("Setters should modify fields correctly")
    void settersShouldModifyFields() {
        ResponseDTO<String> response = new ResponseDTO<>();
        response.setStatusCode(201);
        response.setBody("created");
        response.setExtraArgs(Map.of("id", 1));

        assertEquals(201, response.getStatusCode());
        assertEquals("created", response.getBody());
        assertEquals(1, response.getExtraArgs().get("id"));
    }
}
