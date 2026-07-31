package com.stillfresh.app.authorizationservice.model;

/**
 * Token response for refresh/login-v2 style flows.
 * Includes backward-compatible {@code jwt} field mirroring {@code accessJwt}.
 */
public class AuthTokensResponse {
    private String accessJwt;
    private String refreshToken;
    /** @deprecated Use {@link #accessJwt}. Kept for older clients expecting "jwt". */
    @Deprecated
    private String jwt;

    public AuthTokensResponse() {}

    public AuthTokensResponse(String accessJwt, String refreshToken) {
        this.accessJwt = accessJwt;
        this.refreshToken = refreshToken;
        this.jwt = accessJwt;
    }

    public String getAccessJwt() {
        return accessJwt;
    }

    public void setAccessJwt(String accessJwt) {
        this.accessJwt = accessJwt;
        this.jwt = accessJwt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
        if (this.accessJwt == null) {
            this.accessJwt = jwt;
        }
    }
}

