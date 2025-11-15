package com.maxcogito.auth.dto;

import java.util.Set;

public class SessionMeResponse {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String username;
    private String email;
    private Set<String> roles;

    /** true if last MFA is within the step-up freshness window */
    private boolean mfa;

    /** age in seconds since last MFA (0 if just verified, large number if never) */
    private long mfaAgeSec;

    /** server-side now (epoch seconds) for the client’s convenience */
    private long nowEpochSec;

    public SessionMeResponse() {}

    public SessionMeResponse(String username, String email, Set<String> roles,
                             boolean mfa, long mfaAgeSec, long nowEpochSec) {
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.mfa = mfa;
        this.mfaAgeSec = mfaAgeSec;
        this.nowEpochSec = nowEpochSec;
    }

    // getters/setters

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public boolean isMfa() { return mfa; }
    public void setMfa(boolean mfa) { this.mfa = mfa; }

    public long getMfaAgeSec() { return mfaAgeSec; }
    public void setMfaAgeSec(long mfaAgeSec) { this.mfaAgeSec = mfaAgeSec; }

    public long getNowEpochSec() { return nowEpochSec; }
    public void setNowEpochSec(long nowEpochSec) { this.nowEpochSec = nowEpochSec; }
}

