package com.alidogukan.avora.models;

/** Validated network values sent to the Raspberry Pi. */
public final class NetworkConfigurationRequest {
    public static final String MODE_DHCP = "DHCP";
    public static final String MODE_STATIC = "STATIC";

    public final String requestId;
    public final String mode;
    public final String interfaceName;
    public final String ipAddress;
    public final int prefixLength;
    public final String gateway;
    public final String primaryDns;
    public final String secondaryDns;

    public NetworkConfigurationRequest(String mode, String interfaceName,
                                       String ipAddress, int prefixLength,
                                       String gateway, String primaryDns,
                                       String secondaryDns) {
        this.requestId = java.util.UUID.randomUUID().toString();
        this.mode = safe(mode).toUpperCase(java.util.Locale.ROOT);
        this.interfaceName = safe(interfaceName);
        this.ipAddress = safe(ipAddress);
        this.prefixLength = prefixLength;
        this.gateway = safe(gateway);
        this.primaryDns = safe(primaryDns);
        this.secondaryDns = safe(secondaryDns);
    }

    public static NetworkConfigurationRequest dhcp(String interfaceName) {
        return new NetworkConfigurationRequest(
                MODE_DHCP, interfaceName, "", 24, "", "", "");
    }

    public static NetworkConfigurationRequest fixed(String interfaceName,
                                                    NetworkSettingsValidator.Result value) {
        return new NetworkConfigurationRequest(MODE_STATIC, interfaceName,
                value.ipAddress, value.prefixLength, value.gateway,
                value.primaryDns, value.secondaryDns);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
