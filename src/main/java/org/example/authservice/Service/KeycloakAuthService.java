package org.example.authservice.Service;

import org.example.authservice.Config.KeycloakProperties;
import org.example.authservice.Dto.TokenResponse;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

@Service
public class KeycloakAuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private final KeycloakProperties keycloakProperties;
    private final WebClient webClient;

    public KeycloakAuthService(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Получение нового токена по логину и паролю (grant_type=password).
     */
    public TokenResponse obtainTokenByUsernamePassword(String username, String password) {
        log.info(keycloakProperties.getClientId() + keycloakProperties.getClientSecret());
        return webClient.post()
                .uri(keycloakProperties.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters.fromFormData("grant_type", "password")
                                .with("client_id", keycloakProperties.getClientId())
                                .with("client_secret", keycloakProperties.getClientSecret())
                                .with("username", username)
                                .with("password", password)
                )
                .retrieve()
                .onStatus(
                        httpStatusCode -> httpStatusCode.is4xxClientError(),
                        clientResponse ->
                                clientResponse
                                        .bodyToMono(String.class)
                                        .map(errorBody -> {
                                            System.out.println("Keycloak 4xx error: " + errorBody);
                                            // Возвращаем Throwable (Exception) внутри Mono
                                            return new RuntimeException("4xx: " + errorBody);
                                        })
                )
                .onStatus(
                        httpStatusCode -> httpStatusCode.is5xxServerError(),
                        clientResponse ->
                                clientResponse
                                        .bodyToMono(String.class)
                                        .map(errorBody -> {
                                            System.out.println("Keycloak 5xx error: " + errorBody);
                                            return new RuntimeException("5xx: " + errorBody);
                                        })
                )
                .bodyToMono(TokenResponse.class)
                .block();


    }

    /**
     * Обновление токена по refresh_token (grant_type=refresh_token).
     */
    public TokenResponse refreshToken(String refreshToken) {
        return webClient.post()
                .uri(keycloakProperties.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters.fromFormData("grant_type", "refresh_token")
                                .with("client_id", keycloakProperties.getClientId())
                                .with("client_secret", keycloakProperties.getClientSecret())
                                .with("refresh_token", refreshToken)
                )
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();
    }
}

