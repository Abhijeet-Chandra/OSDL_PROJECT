package com.hotel.auth;

import com.hotel.model.UserRole;

/**
 * Current logged-in user; passed into the main shell after successful login.
 */
public record UserSession(int userId, String username, UserRole role, Integer linkedGuestId) {

    public boolean isCustomer() {
        return role == UserRole.CUSTOMER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
