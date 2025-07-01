package se.buscatcher.buscatcherapi.Model;

import java.io.Serializable;

public class GoogleTokens implements Serializable {
    private final Long expiresIn;
    private String accessToken;
    private String refreshToken;

    public GoogleTokens(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}
