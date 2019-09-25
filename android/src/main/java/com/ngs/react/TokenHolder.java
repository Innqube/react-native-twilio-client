package com.ngs.react;

public class TokenHolder {

    private static TokenHolder instance;
    private String token;

    private TokenHolder() {

    }

    public static TokenHolder get() {
        if (instance == null) {
            instance = new TokenHolder();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
