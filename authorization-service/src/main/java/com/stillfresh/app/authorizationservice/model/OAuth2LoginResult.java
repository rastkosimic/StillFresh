package com.stillfresh.app.authorizationservice.model;

/**
 * Result of OAuth2 login: access + refresh tokens and whether the account was previously deleted and has been reactivated.
 */
public class OAuth2LoginResult {
    private final String accessJwt;
    private final String refreshToken;
    private final boolean accountWasDeleted;

    public OAuth2LoginResult(String accessJwt, String refreshToken, boolean accountWasDeleted) {
        this.accessJwt = accessJwt;
        this.refreshToken = refreshToken;
        this.accountWasDeleted = accountWasDeleted;
    }

    public String getAccessJwt() {
        return accessJwt;
    }

    // Backward-compatible alias for codepaths still expecting "jwt".
    public String getJwt() {
        return accessJwt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isAccountWasDeleted() {
        return accountWasDeleted;
    }
}
