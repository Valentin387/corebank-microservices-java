package com.corebank.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain value object representing login credentials.
 */
@Getter
@AllArgsConstructor
public class Credentials {
    private final String username;
    private final String password;

    /**
     * Validate credentials against mock data.
     * In a real system this would delegate to a user store.
     */
    public boolean isValid() {
        return "user".equals(username) && "password".equals(password);
    }
}
