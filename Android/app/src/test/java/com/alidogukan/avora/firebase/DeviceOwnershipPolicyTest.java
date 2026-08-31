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
        claims.put("avora_device_id", "smartgarden-001");

        assertTrue(DeviceOwnershipPolicy.ownsDevice(claims, "smartgarden-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(claims, "smartgarden-002"));
    }

    @Test
    public void rejectsMissingMalformedOrBlankClaims() {
        assertFalse(DeviceOwnershipPolicy.ownsDevice(null, "smartgarden-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(Map.of(), "smartgarden-001"));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(
                Map.of("avora_device_id", 1),
                "smartgarden-001"
        ));
        assertFalse(DeviceOwnershipPolicy.ownsDevice(
                Map.of("avora_device_id", "smartgarden-001"),
                " "
        ));
    }
}
