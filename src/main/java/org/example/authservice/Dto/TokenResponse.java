package org.example.authservice.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

   public String getAccessToken() {
       return accessToken;
   }

   public void setAccessToken(String accessToken) {
       this.accessToken = accessToken;
   }

   public String getRefreshToken() {
       return refreshToken;
   }

   public void setRefreshToken(String refreshToken) {
       this.refreshToken = refreshToken;
   }

   public String getTokenType() {
       return tokenType;
   }

   public void setTokenType(String tokenType) {
       this.tokenType = tokenType;
   }

   public Long getExpiresIn() {
       return expiresIn;
   }

   public void setExpiresIn(Long expiresIn) {
       this.expiresIn = expiresIn;
   }

   public Long getRefreshExpiresIn() {
       return refreshExpiresIn;
   }

   public void setRefreshExpiresIn(Long refreshExpiresIn) {
       this.refreshExpiresIn = refreshExpiresIn;
   }

}


