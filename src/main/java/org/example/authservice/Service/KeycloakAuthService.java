package org.example.authservice.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.authservice.Client.UserServiceClient;
import org.example.authservice.Config.KeycloakProperties;
import org.example.authservice.Dto.TokenResponse;
import org.example.userservice.ui.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class KeycloakAuthService {
    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private final KeycloakProperties keycloakProperties;
    private final WebClient webClient;
    private final UserServiceClient userServiceClient;

    public KeycloakAuthService(KeycloakProperties keycloakProperties, UserServiceClient userServiceClient) {
        this.keycloakProperties = keycloakProperties;
        this.webClient = WebClient.builder().build();
        this.userServiceClient = userServiceClient;
    }

    /**
     * Получение нового токена по логину и паролю (grant_type=password).
     */
    public TokenResponse obtainTokenByUsernamePassword(String username, String password) {
        log.info("Obtaining token for user: {}", username);
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
                                        .flatMap(errorBody -> {
                                            log.error("Keycloak 4xx error: {}", errorBody);
                                            return Mono.error(new RuntimeException("4xx: " + errorBody));
                                        })
                )
                .onStatus(
                        httpStatusCode -> httpStatusCode.is5xxServerError(),
                        clientResponse ->
                                clientResponse
                                        .bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Keycloak 5xx error: {}", errorBody);
                                            return Mono.error(new RuntimeException("5xx: " + errorBody));
                                        })
                )
                .bodyToMono(TokenResponse.class)
                .block();
    }

    /**
     * Обновление токена по refresh_token (grant_type=refresh_token).
     */
    public TokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
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

    /**
     * Регистрация нового пользователя.
     */
    public void registerUser(String email, String password) {
        log.info("Registering user in Keycloak: {}", email);

        // 1. Получаем административный токен
        String clientToken = getClientToken();
        log.info("Client token: {}", clientToken);

        // 2. Регистрация в Keycloak
        webClient.post()
                .uri(keycloakProperties.getUserRegistrationEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserRegistrationRequest(email, password))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Keycloak 4xx error: {}", errorBody);
                        return Mono.error(new RuntimeException("4xx: " + errorBody));
                    });
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Keycloak 5xx error: {}", errorBody);
                        return Mono.error(new RuntimeException("5xx: " + errorBody));
                    });
                })
                .bodyToMono(Void.class)
                .block();

        // 3. Создание записи о пользователе в UserService
        UserDTO userDto = new UserDTO();
        userDto.setEmail(email);
        userDto.setUsername(email);
        try {
            userServiceClient.createUser(userDto);
        } catch (Exception e) {
            log.error("Failed to create user in UserService", e);
            throw e;
        }
    }

    private String getClientToken() {
        return webClient.post()
                .uri(keycloakProperties.getAuthServerUrl() + "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", keycloakProperties.getClientId())
                        .with("client_secret", keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .block();
    }

    private static class UserRegistrationRequest {
        private final String email;
        private final String username;
        private final List<Credential> credentials;

        public UserRegistrationRequest(String email, String password) {
            this.email = email;
            this.username = email;
            this.credentials = List.of(new Credential(password));
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public List<Credential> getCredentials() {
            return credentials;
        }

        private static class Credential {
            private final String type = "password";
            private final String value;
            private final boolean temporary = false;

            public Credential(String value) {
                this.value = value;
            }

            public String getType() {
                return type;
            }

            public String getValue() {
                return value;
            }

            public boolean isTemporary() {
                return temporary;
            }
        }
    }
}