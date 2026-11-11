package com.cleanroom.remotedesktop.protocol.model;

public class SessionModels {

    public static class RegisterHostReq {
        private String preferredId;

        public RegisterHostReq() {}
        public RegisterHostReq(String preferredId) { this.preferredId = preferredId; }

        public String getPreferredId() { return preferredId; }
        public void setPreferredId(String preferredId) { this.preferredId = preferredId; }
    }

    public static class RegisterHostResp {
        private boolean success;
        private String hostId;
        private String message;

        public RegisterHostResp() {}
        public RegisterHostResp(boolean success, String hostId, String message) {
            this.success = success;
            this.hostId = hostId;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getHostId() { return hostId; }
        public void setHostId(String hostId) { this.hostId = hostId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ConnectPartnerReq {
        private String partnerId;

        public ConnectPartnerReq() {}
        public ConnectPartnerReq(String partnerId) { this.partnerId = partnerId; }

        public String getPartnerId() { return partnerId; }
        public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    }

    public static class AuthChallenge {
        private String salt;
        private String nonce;

        public AuthChallenge() {}
        public AuthChallenge(String salt, String nonce) {
            this.salt = salt;
            this.nonce = nonce;
        }

        public String getSalt() { return salt; }
        public void setSalt(String salt) { this.salt = salt; }
        public String getNonce() { return nonce; }
        public void setNonce(String nonce) { this.nonce = nonce; }
    }

    public static class AuthResponse {
        private String partnerId;
        private String nonce;
        private String hmacProof;

        public AuthResponse() {}
        public AuthResponse(String partnerId, String nonce, String hmacProof) {
            this.partnerId = partnerId;
            this.nonce = nonce;
            this.hmacProof = hmacProof;
        }

        public String getPartnerId() { return partnerId; }
        public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
        public String getNonce() { return nonce; }
        public void setNonce(String nonce) { this.nonce = nonce; }
        public String getHmacProof() { return hmacProof; }
        public void setHmacProof(String hmacProof) { this.hmacProof = hmacProof; }
    }

    public static class SessionAccepted {
        private String sessionId;
        private String partnerId;
        private boolean viewOnly;

        public SessionAccepted() {}
        public SessionAccepted(String sessionId, String partnerId, boolean viewOnly) {
            this.sessionId = sessionId;
            this.partnerId = partnerId;
            this.viewOnly = viewOnly;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getPartnerId() { return partnerId; }
        public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
        public boolean isViewOnly() { return viewOnly; }
        public void setViewOnly(boolean viewOnly) { this.viewOnly = viewOnly; }
    }

    public static class SessionRejected {
        private String reason;

        public SessionRejected() {}
        public SessionRejected(String reason) { this.reason = reason; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SessionTerminate {
        private String reason;

        public SessionTerminate() {}
        public SessionTerminate(String reason) { this.reason = reason; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}

