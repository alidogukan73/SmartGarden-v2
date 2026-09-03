package com.alidogukan.avora.models;

import com.google.firebase.database.PropertyName;

/** Progress/result of the latest Raspberry Pi network change. */
public final class NetworkConfigurationResult {
    private String requestId = "";
    private String status = "";
    private String message = "";
    private String appliedIp = "";
    private long updatedAtEpoch;

    public NetworkConfigurationResult() { }

    @PropertyName("request_id")
    public String getRequestId() { return requestId; }
    @PropertyName("request_id")
    public void setRequestId(String value) { requestId = safe(value); }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = safe(value); }

    public String getMessage() { return message; }
    public void setMessage(String value) { message = safe(value); }

    @PropertyName("applied_ip")
    public String getAppliedIp() { return appliedIp; }
    @PropertyName("applied_ip")
    public void setAppliedIp(String value) { appliedIp = safe(value); }

    @PropertyName("updated_at_epoch")
    public long getUpdatedAtEpoch() { return updatedAtEpoch; }
    @PropertyName("updated_at_epoch")
    public void setUpdatedAtEpoch(long value) { updatedAtEpoch = value; }

    public boolean isInProgress() {
        return "PENDING".equals(status) || "VALIDATING".equals(status)
                || "APPLYING".equals(status) || "VERIFYING".equals(status);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
