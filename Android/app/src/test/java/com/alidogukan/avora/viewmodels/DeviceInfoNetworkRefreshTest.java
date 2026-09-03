package com.alidogukan.avora.viewmodels;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.DeviceNetworkStatus;

import org.junit.Test;

public final class DeviceInfoNetworkRefreshTest {
    @Test
    public void staleStaticStateDoesNotCompleteDhcpRefresh() {
        DeviceNetworkStatus current = status("STATIC", "192.168.1.99");

        assertFalse(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                current, "DHCP", "192.168.1.109"));
    }

    @Test
    public void publishedDhcpStateCompletesRefresh() {
        DeviceNetworkStatus current = status("DHCP", "192.168.1.109");

        assertTrue(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                current, "DHCP", "192.168.1.109"));
    }

    @Test
    public void dhcpCanMatchWhenBackendDoesNotReturnAppliedIp() {
        DeviceNetworkStatus current = status("DHCP", "192.168.1.109");

        assertTrue(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                current, "DHCP", ""));
    }

    @Test
    public void wrongStaticAddressDoesNotCompleteRefresh() {
        DeviceNetworkStatus current = status("STATIC", "192.168.1.98");

        assertFalse(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                current, "STATIC", "192.168.1.99"));
    }

    @Test
    public void publishedStaticStateCompletesRefresh() {
        DeviceNetworkStatus current = status("STATIC", "192.168.1.99");

        assertTrue(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                current, "STATIC", "192.168.1.99"));
    }

    @Test
    public void missingStateNeverCompletesRefresh() {
        assertFalse(DeviceInfoViewModel.representsAppliedNetworkConfiguration(
                null, "STATIC", "192.168.1.99"));
    }

    private static DeviceNetworkStatus status(String mode, String ip) {
        DeviceNetworkStatus value = new DeviceNetworkStatus();
        value.setMode(mode);
        value.setIpAddress(ip);
        return value;
    }
}
