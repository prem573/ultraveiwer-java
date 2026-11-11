package com.cleanroom.remotedesktop.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, AttemptTracker> trackers = new ConcurrentHashMap<>();

    public RateLimiter(int maxAttempts, long windowMillis) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    public boolean isAllowed(String key) {
        cleanExpired();
        AttemptTracker tracker = trackers.get(key);
        if (tracker == null) {
            return true;
        }
        return tracker.count.get() < maxAttempts;
    }

    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        trackers.compute(key, (k, existing) -> {
            if (existing == null || now - existing.firstAttemptTime > windowMillis) {
                return new AttemptTracker(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        trackers.remove(key);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        trackers.entrySet().removeIf(entry -> now - entry.getValue().firstAttemptTime > windowMillis);
    }

    private static class AttemptTracker {
        final long firstAttemptTime;
        final AtomicInteger count = new AtomicInteger(1);

        AttemptTracker(long firstAttemptTime) {
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
