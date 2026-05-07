package com.corebank.auth.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsTest {

    @Test
    @DisplayName("isValid should return true for correct credentials")
    void isValidShouldReturnTrueForCorrectCredentials() {
        Credentials credentials = new Credentials("user", "password");
        assertTrue(credentials.isValid());
    }

    @Test
    @DisplayName("isValid should return false for wrong username")
    void isValidShouldReturnFalseForWrongUsername() {
        Credentials credentials = new Credentials("wrong", "password");
        assertFalse(credentials.isValid());
    }

    @Test
    @DisplayName("isValid should return false for wrong password")
    void isValidShouldReturnFalseForWrongPassword() {
        Credentials credentials = new Credentials("user", "wrong");
        assertFalse(credentials.isValid());
    }

    @Test
    @DisplayName("getters should return provided values")
    void gettersShouldReturnProvidedValues() {
        Credentials credentials = new Credentials("testuser", "testpass");
        assertEquals("testuser", credentials.getUsername());
        assertEquals("testpass", credentials.getPassword());
    }
}
