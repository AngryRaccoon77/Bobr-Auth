package org.example.authservice.Controller;

import org.example.authservice.Dto.AuthRequest;
import org.example.authservice.Dto.RefreshRequest;
import org.example.authservice.Dto.TokenResponse;
import org.example.authservice.Service.KeycloakAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;

    public AuthController(KeycloakAuthService keycloakAuthService) {
        this.keycloakAuthService = keycloakAuthService;
    }

    /**
     * 1) Получить токен по логину/паролю.
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(@RequestBody AuthRequest authRequest) {
        TokenResponse tokenResponse = keycloakAuthService
                .obtainTokenByUsernamePassword(authRequest.getUsername(), authRequest.getPassword());
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 2) Обновить токен по refresh_token.
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        TokenResponse tokenResponse = keycloakAuthService
                .refreshToken(refreshRequest.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }
}
