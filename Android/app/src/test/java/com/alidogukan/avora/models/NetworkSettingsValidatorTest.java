package com.alidogukan.avora.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NetworkSettingsValidatorTest {
    @Test
    public void acceptsResidentialStaticConfiguration() {
        NetworkSettingsValidator.Result result = NetworkSettingsValidator.validateStatic(
                "192.168.1.50", "255.255.255.0", "192.168.1.1",
                "1.1.1.1", "8.8.8.8");

        assertTrue(result.valid);
        assertEquals(24, result.prefixLength);
        assertEquals("192.168.1.50", result.ipAddress);
    }

    @Test
    public void rejectsNonContiguousMask() {
        NetworkSettingsValidator.Result result = NetworkSettingsValidator.validateStatic(
                "192.168.1.50", "255.0.255.0", "192.168.1.1",
                "1.1.1.1", "");

        assertFalse(result.valid);
        assertEquals(NetworkSettingsValidator.Error.SUBNET_INVALID, result.error);
    }

    @Test
    public void rejectsGatewayOutsideSubnet() {
        NetworkSettingsValidator.Result result = NetworkSettingsValidator.validateStatic(
                "192.168.1.50", "255.255.255.0", "192.168.2.1",
                "1.1.1.1", "");

        assertFalse(result.valid);
        assertEquals(NetworkSettingsValidator.Error.GATEWAY_OUTSIDE_SUBNET, result.error);
    }

    @Test
    public void requiresDnsForFirebaseRecovery() {
        NetworkSettingsValidator.Result result = NetworkSettingsValidator.validateStatic(
                "192.168.1.50", "255.255.255.0", "192.168.1.1", "", "");

        assertFalse(result.valid);
        assertEquals(NetworkSettingsValidator.Error.DNS_REQUIRED, result.error);
    }

    @Test
    public void convertsPrefixToMask() {
        assertEquals("255.255.255.0",
                NetworkSettingsValidator.subnetMaskForPrefix(24));
        assertEquals("255.255.0.0",
                NetworkSettingsValidator.subnetMaskForPrefix(16));
    }
}
