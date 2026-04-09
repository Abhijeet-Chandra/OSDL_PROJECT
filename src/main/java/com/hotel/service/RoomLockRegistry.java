package com.hotel.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-room lock registry for thread synchronization.
 */
public final class RoomLockRegistry {

    private static final RoomLockRegistry INSTANCE = new RoomLockRegistry();

    private final Map<Integer, Object> roomLocks = new HashMap<>();

    private RoomLockRegistry() {
    }

    public static RoomLockRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Demonstrates use of the {@code synchronized} keyword.
     * Returns a stable lock object per room id.
     */
    public synchronized Object lockForRoom(int roomId) {
        return roomLocks.computeIfAbsent(roomId, k -> new Object());
    }
}

