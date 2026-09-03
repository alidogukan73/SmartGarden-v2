package com.alidogukan.avora.models;

import com.google.firebase.database.PropertyName;

/** Current Raspberry Pi network state published by the backend. */
public final class DeviceNetworkStatus {
    private boolean supported;
    private String interfaceName = "";
    private String connectionName = "";
    private String mode = "";
    private String ipAddress = "";
    private long prefixLength;
    private String subnetMask = "";
    private String gateway = "";
    private String primaryDns = "";
    private String secondaryDns = "";
    private String tailscaleIp = "";
    private long updatedAtEpoch;

    public DeviceNetworkStatus() { }

    public boolean isSupported() { return supported; }
    public void setSupported(boolean supported) { this.supported = supported; }

    @PropertyName("interface")
    public String getInterfaceName() { return interfaceName; }
    @PropertyName("interface")
    public void setInterfaceName(String value) { interfaceName = safe(value); }

    @PropertyName("connection_name")
    public String getConnectionName() { return connectionName; }
    @PropertyName("connection_name")
    public void setConnectionName(String value) { connectionName = safe(value); }

    public String getMode() { return mode; }
    public void setMode(String value) { mode = safe(value); }

    @PropertyName("ip_address")
    public String getIpAddress() { return ipAddress; }
    @PropertyName("ip_address")
    public void setIpAddress(String value) { ipAddress = safe(value); }

    @PropertyName("prefix_length")
    public long getPrefixLength() { return prefixLength; }
    @PropertyName("prefix_length")
    public void setPrefixLength(long value) { prefixLength = value; }

    @PropertyName("subnet_mask")
    public String getSubnetMask() { return subnetMask; }
    @PropertyName("subnet_mask")
    public void setSubnetMask(String value) { subnetMask = safe(value); }

    public String getGateway() { return gateway; }
    public void setGateway(String value) { gateway = safe(value); }

    @PropertyName("primary_dns")
    public String getPrimaryDns() { return primaryDns; }
    @PropertyName("primary_dns")
    public void setPrimaryDns(String value) { primaryDns = safe(value); }

    @PropertyName("secondary_dns")
    public String getSecondaryDns() { return secondaryDns; }
    @PropertyName("secondary_dns")
    public void setSecondaryDns(String value) { secondaryDns = safe(value); }

    @PropertyName("tailscale_ip")
    public String getTailscaleIp() { return tailscaleIp; }
    @PropertyName("tailscale_ip")
    public void setTailscaleIp(String value) { tailscaleIp = safe(value); }

    @PropertyName("updated_at_epoch")
    public long getUpdatedAtEpoch() { return updatedAtEpoch; }
    @PropertyName("updated_at_epoch")
    public void setUpdatedAtEpoch(long value) { updatedAtEpoch = value; }

    private static String safe(String value) { return value == null ? "" : value; }
}
