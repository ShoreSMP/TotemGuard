package com.deathmotion.totemguard.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticViolationCounterTest {
    @Test void normalAcceptedFlagsAccumulateWithoutChangingGenerations() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        for (int i = 0; i < 30; i++) counter.record(10, false, 10);
        assertEquals(30, counter.count(10));
    }

    @Test void restrictedFlagsDoNotAccumulateAutomaticBanEligibility() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        for (int i = 0; i < 1000; i++) counter.record(11, true, 11);
        assertEquals(0, counter.count(11));
    }

    @Test void releaseNeedsFreshFlagsInsteadOfReusingEarlierViolations() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        for (int i = 0; i < 30; i++) counter.record(10, false, 10);
        counter.record(11, true, 11);
        counter.record(12, false, 12);
        assertEquals(1, counter.count(12));
        assertEquals(0, counter.count(10));
        assertEquals(0, counter.count(11));
    }

    @Test void delayedRestrictedFlagsDoNotBecomeFreshFlagsAfterRelease() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        counter.record(11, true, 11);
        counter.record(12, false, 12);
        counter.record(12, false, 11);
        assertEquals(1, counter.count(12), "Old queued flags neither add to nor erase new normal history");
    }

    @Test void requestWithAnOldRevisionCannotReadCurrentEligibility() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        counter.record(12, false, 12);
        assertEquals(0, counter.count(11));
    }

    @Test void reconnectStartsANewAutomaticBanHistory() {
        AutomaticViolationCounter counter = new AutomaticViolationCounter();
        counter.record(12, false, 12);
        counter.record(20, false, 20);
        assertEquals(1, counter.count(20));
        assertEquals(0, counter.count(12));
    }
}
