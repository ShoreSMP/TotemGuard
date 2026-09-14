package com.deathmotion.totemguard.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class StaffIsolationPolicyTest {
    private final Object owner = new Object();
    private final UUID subject = UUID.randomUUID();
    @AfterEach void cleanup() { StaffIsolationPolicy.clear(owner); }

    @Test void absentStaffPreservesExistingOutputs() {
        assertFalse(StaffIsolationPolicy.suppressed(subject));
        assertTrue(StaffIsolationPolicy.allowsBan(subject, 0));
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "ban Player 1d reason", 0, true));
        assertFalse(StaffIsolationPolicy.dispatchAutoban(subject, "autoban totemguard Player 1d reason", 0));
    }

    @Test void capturesAndRechecksGenerationAfterScheduling() {
        AtomicLong revision = new AtomicLong(1);
        StaffIsolationPolicy.install(owner, id -> false, id -> revision.get(),
                (id, token) -> token == revision.get(), request -> true);
        long captured = StaffIsolationPolicy.revision(subject);
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "autoban totemguard Player 1d reason", captured, true));
        revision.set(3); // Isolation began and then ended while queued.
        assertFalse(StaffIsolationPolicy.allowsCommand(subject, "autoban totemguard Player 1d reason", captured, true));
    }

    @Test void restrictedSubjectsKeepCorrectiveAndKickCommands() {
        StaffIsolationPolicy.install(owner, id -> true, id -> 1, (id, token) -> false, request -> true);
        for (String command : List.of("kick Player invalid packet", "teleport Player spawn", "custom-correction Player")) {
            assertTrue(StaffIsolationPolicy.allowsCommand(subject, command, 1, false), command);
        }
        for (String command : List.of("ban Player 1d reason", "staff:autoban totemguard Player 1d reason", "anticheatbc Player")) {
            assertFalse(StaffIsolationPolicy.allowsCommand(subject, command, 1, true), command);
        }
    }

    @Test void delayedMixedBatchCannotAnnounceItsOldBanAfterRelease() {
        AtomicLong revision = new AtomicLong(1);
        StaffIsolationPolicy.install(owner, id -> false, id -> revision.get(),
                (id, token) -> token == revision.get(), request -> true);
        long captured = StaffIsolationPolicy.revision(subject);
        assertTrue(StaffIsolationPolicy.allowsNotification(subject, captured));
        revision.set(3); // The subject is normal again, but this mixed batch predates isolation.
        for (String command : List.of("anticheatbc Player", "[webhook]", "[alert]", "[proxy]")) {
            assertFalse(StaffIsolationPolicy.allowsCommand(subject, command, captured, true), command);
        }
        assertFalse(StaffIsolationPolicy.allowsNotification(subject, captured), "The direct Discord publisher must reject the same old batch");
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "kick Player invalid packet", captured, false));
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "custom-correction Player", captured, false));
        assertTrue(StaffIsolationPolicy.allowsNotification(subject, revision.get()), "New normal-generation notifications resume");
    }

    @Test void currentRestrictedNotificationsAndLookupFailuresStaySuppressed() {
        StaffIsolationPolicy.install(owner, id -> true, id -> 3, (id, token) -> false, request -> true);
        assertFalse(StaffIsolationPolicy.allowsNotification(subject, 3));
        StaffIsolationPolicy.install(owner, id -> false, id -> { throw new IllegalStateException(); },
                (id, token) -> false, request -> true);
        assertFalse(StaffIsolationPolicy.allowsNotification(subject, 3));
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "kick Player bad packet", 3, true));
    }

    @Test void insufficientFreshFlagsBlockOnlyAutomaticBans() {
        StaffIsolationPolicy.install(owner, id -> false, id -> 3, (id, token) -> token == 3, request -> true);
        assertFalse(StaffIsolationPolicy.allowsCommand(subject, "autoban totemguard Player 1d reason", 3, false));
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "kick Player bad packet", 3, false));
    }

    @Test void forwardsCreationTimeTokenWithoutFallingBackToConsole() {
        AtomicLong token = new AtomicLong(-1);
        StaffIsolationPolicy.install(owner, id -> false, id -> 3, (id, captured) -> true,
                request -> { token.set(((Number) request[2]).longValue()); return true; });
        assertTrue(StaffIsolationPolicy.dispatchAutoban(subject, "/staff:autoban\ttotemguard Player 1d reason", 2));
        assertEquals(2, token.get());
        assertFalse(StaffIsolationPolicy.dispatchAutoban(subject, "kick Player invalid packet", 2));
    }

    @Test void callbackFailureBlocksOnlyOutputsAndDoesNotRedispatch() {
        StaffIsolationPolicy.install(owner, id -> { throw new IllegalStateException(); }, id -> 3,
                (id, token) -> { throw new IllegalStateException(); }, request -> { throw new IllegalStateException(); });
        assertTrue(StaffIsolationPolicy.suppressed(subject));
        assertFalse(StaffIsolationPolicy.allowsBan(subject, 3));
        assertTrue(StaffIsolationPolicy.dispatchAutoban(subject, "autoban totemguard Player 1d reason", 3));
        assertTrue(StaffIsolationPolicy.allowsCommand(subject, "kick Player bad packet", 3, true));
    }

    @Test void unrelatedOwnerCannotRemoveTheIntegration() {
        StaffIsolationPolicy.install(owner, id -> true, id -> 3, (id, token) -> false, request -> true);
        StaffIsolationPolicy.clear(new Object());
        assertTrue(StaffIsolationPolicy.suppressed(subject));
        StaffIsolationPolicy.clear(owner);
        assertFalse(StaffIsolationPolicy.suppressed(subject));
    }

    @Test void renderedMessagesAreNeverGuessedAsPunishmentActions() {
        assertFalse(StaffIsolationPolicy.isBan("Player failed AutoTotem 50"));
        assertFalse(StaffIsolationPolicy.isNotification("<red>Player has been banned"));
        assertTrue(StaffIsolationPolicy.isNotification("[webhook]"));
    }
}
