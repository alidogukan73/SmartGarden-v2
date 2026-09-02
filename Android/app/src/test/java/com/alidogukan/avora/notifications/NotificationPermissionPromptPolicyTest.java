package com.alidogukan.avora.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NotificationPermissionPromptPolicyTest {
    private static final long NOW = 2_000_000_000_000L;

    @Test
    public void firstRequestIsShownWhenPermissionIsMissing() {
        assertTrue(NotificationPermissionPromptPolicy.shouldPrompt(
                false, false, 0L, NOW));
    }

    @Test
    public void grantedPermissionIsNeverRequested() {
        assertFalse(NotificationPermissionPromptPolicy.shouldPrompt(
                true, false, 0L, NOW));
    }

    @Test
    public void denialIsNotRepeatedBeforeSevenDays() {
        long sixDaysAgo = NOW - NotificationPermissionPromptPolicy.RETRY_INTERVAL_MILLIS
                + 1L;
        assertFalse(NotificationPermissionPromptPolicy.shouldPrompt(
                false, true, sixDaysAgo, NOW));
    }

    @Test
    public void denialMayBeRequestedAgainAfterSevenDays() {
        long sevenDaysAgo = NOW - NotificationPermissionPromptPolicy.RETRY_INTERVAL_MILLIS;
        assertTrue(NotificationPermissionPromptPolicy.shouldPrompt(
                false, true, sevenDaysAgo, NOW));
    }

    @Test
    public void clockRollbackDoesNotCauseImmediateRepeat() {
        assertFalse(NotificationPermissionPromptPolicy.shouldPrompt(
                false, true, NOW + 1L, NOW));
    }
}
