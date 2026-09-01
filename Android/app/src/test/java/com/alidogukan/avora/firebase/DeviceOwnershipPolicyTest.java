package com.alidogukan.avora.firebase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class DeviceOwnershipPolicyTest {
    @Test
    public void acceptsOnlyTheExactClaimedDevice() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("avora_device_id", "avora-001");

        assertTrue(DeviceOwnershipPolicy.ownsDevice(claims, "avora-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(claims, "avora-002"));
    }

    @Test
    public void rejectsMissingMalformedOrBlankClaims() {
        assertFalse(DeviceOwnershipPolicy.ownsDevice(null, "avora-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(Map.of(), "avora-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(
                Map.of("avora_device_id", 1),
                "avora-001"
        ));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(
                Map.of("avora_device_id", "avora-001"),
                " "
        ));
    }
}
