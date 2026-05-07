package com.corebank.auth.application.port.input;

/**
 * Input port (use case) for client authentication.
 * Implemented by the application service, called by the web adapter.
 */
public interface AuthenticateUseCase {

    /**
     * Authenticate a client and return a signed JWT.
     *
     * @param username      client username
     * @param password      client password
     * @param custIdentNum  customer identification number (banking header)
     * @param custIdentType customer identification type (banking header)
     * @return signed JWT token string
     * @throws SecurityException if credentials are invalid
     */
    String authenticate(String username, String password, String custIdentNum, String custIdentType);
}
