package com.hotel.model;

public class Room {

    private int id;
    private String roomNumber;
    private String roomType;
    private int capacity;
    private int dailyRateCents;
    private RoomStatus status;

    public Room() {
    }

    public Room(int id, String roomNumber, String roomType, int capacity, int dailyRateCents, RoomStatus status) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.dailyRateCents = dailyRateCents;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getDailyRateCents() {
        return dailyRateCents;
    }

    public void setDailyRateCents(int dailyRateCents) {
        this.dailyRateCents = dailyRateCents;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return roomNumber + " (" + roomType + ")";
    }
}

