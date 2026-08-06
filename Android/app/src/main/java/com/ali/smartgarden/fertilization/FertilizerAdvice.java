package com.ali.smartgarden.fertilization;

import java.util.List;

public class FertilizerAdvice {
    private final String zoneTitle, status, reason, context;
    private final List<String> candidates;
    private final List<String> risks;
    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks) {
        this.zoneTitle = zoneTitle; this.status = status; this.reason = reason;
        this.context = context; this.candidates = candidates; this.risks = risks;
    }
    public String getZoneTitle() { return zoneTitle; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getContext() { return context; }
    public List<String> getCandidates() { return candidates; }
    public List<String> getRisks() { return risks; }
}
