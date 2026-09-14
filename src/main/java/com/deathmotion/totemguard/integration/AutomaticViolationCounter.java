package com.deathmotion.totemguard.integration;

/** Automatic-ban history only; never changes a check's detector violation counter. */
public final class AutomaticViolationCounter {
    private long revision = Long.MIN_VALUE;
    private int eligible;

    public synchronized void record(long currentRevision, boolean restricted, long flagRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            eligible = 0;
        }
        if (restricted) eligible = 0;
        else if (flagRevision == currentRevision && eligible < Integer.MAX_VALUE) eligible++;
    }

    public synchronized int count(long currentRevision) {
        return revision == currentRevision ? eligible : 0;
    }
}
