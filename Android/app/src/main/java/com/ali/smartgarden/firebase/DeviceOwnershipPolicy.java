package com.ali.smartgarden.firebase;

import java.util.Map;


/** Pure policy for matching a signed Firebase token to one AVORA device. */
public final class DeviceOwnershipPolicy {
    static final String DEVICE_OWNER_CLAIM = "avora_device_id";

    private DeviceOwnershipPolicy() { }

    public static boolean ownsDevice(Map<String, Object> claims, String deviceId) {
        if (claims == null || deviceId == null || deviceId.isBlank()) return false;
        Object claimedDevice = claims.get(DEVICE_OWNER_CLAIM);
        return claimedDevice instanceof String && deviceId.equals(claimedDevice);
    }
}
