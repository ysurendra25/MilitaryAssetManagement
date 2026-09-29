package com.mams.dto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** The logged-in user, read from the session. */
public record SessionUser(String username, String role, Long baseId) {

    public static SessionUser from(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return null;
        }
        return new SessionUser(
                (String) session.getAttribute("username"),
                (String) session.getAttribute("role"),
                (Long) session.getAttribute("baseId"));
    }
}
