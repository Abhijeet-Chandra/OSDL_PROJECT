package com.hotel.model;

import java.time.LocalDate;

/**
 * Lightweight view model for displaying bookings in a TableView.
 */
public class BookingInfo {
    private final int id;
    private final String guestName;
    private final int roomId;
    private final String roomNumber;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final BookingStatus status;

    public BookingInfo(
            int id,
            String guestName,
            int roomId,
            String roomNumber,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            BookingStatus status
    ) {
        this.id = id;
        this.guestName = guestName;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getGuestName() {
        return guestName;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public BookingStatus getStatus() {
        return status;
    }
}

