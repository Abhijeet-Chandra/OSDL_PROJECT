package com.hotel.controller;

import com.hotel.auth.UserSession;

public interface UserSessionAware {
    void setUserSession(UserSession session);
}
