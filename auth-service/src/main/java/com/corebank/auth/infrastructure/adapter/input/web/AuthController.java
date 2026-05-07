package com.corebank.auth.infrastructure.adapter.input.web;

import com.corebank.auth.application.port.input.AuthenticateUseCase;
import com.corebank.commons.dto.LoginRequestDTO;
import com.corebank.commons.model.ResponseDTO;
import com.corebank.commons.security.HeaderConstants;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Web adapter (input) for authentication.
 * Preserves the identical API contract from Phase 1.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase) {
        this.authenticateUseCase = authenticateUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<String>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            @RequestHeader(value = HeaderConstants.X_CUST_IDENT_NUM, required = false) String custIdentNum,
            @RequestHeader(value = HeaderConstants.X_CUST_IDENT_TYPE, required = false) String custIdentType) {

        String token = authenticateUseCase.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                custIdentNum,
                custIdentType
        );

        return ResponseEntity.ok(ResponseDTO.success(token));
    }
}
