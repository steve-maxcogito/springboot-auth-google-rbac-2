package com.maxcogito.auth.dto;

public class ClientCredentialsToken {
    private String token_type;
    private long   expires_in;
    private long   ext_expires_in;
    private String access_token;

    public String getToken_type() { return token_type; }
    public void setToken_type(String token_type) { this.token_type = token_type; }

    public long getExpires_in() { return expires_in; }
    public void setExpires_in(long expires_in) { this.expires_in = expires_in; }

    public long getExt_expires_in() { return ext_expires_in; }
    public void setExt_expires_in(long ext_expires_in) { this.ext_expires_in = ext_expires_in; }

    public String getAccess_token() { return access_token; }
    public void setAccess_token(String access_token) { this.access_token = access_token; }

}
