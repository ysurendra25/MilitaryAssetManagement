package com.mams.config;

import com.mams.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Role-based access control, enforced server-side on every /api request.
 *
 * Rules:
 *  - /api/auth/**        : public (login/logout/me)
 *  - /api/meta/**        : any authenticated user
 *  - /api/purchases/**   : ADMIN, COMMANDER, LOGISTICS
 *  - /api/transfers/**   : ADMIN, COMMANDER, LOGISTICS
 *  - /api/dashboard/**   : ADMIN, COMMANDER
 *  - /api/assignments/** : ADMIN, COMMANDER
 *  - /api/expenditures/**: ADMIN, COMMANDER
 *  - /api/audit/**       : ADMIN only
 *
 * Base-level scoping (a commander sees and acts only on their own base) is
 * enforced inside the services, not just here.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> ALL =
            Set.of("ADMIN", "COMMANDER", "LOGISTICS");
    private static final Set<String> STAFF =
            Set.of("ADMIN", "COMMANDER");
    private static final Set<String> ADMIN_ONLY =
            Set.of("ADMIN");

    private final AuditService auditService;

    public AuthInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // login/logout and static assets are public
        if (!uri.startsWith("/api/") || uri.startsWith("/api/auth/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute("username");
        String role = session == null ? null : (String) session.getAttribute("role");

        if (username == null) {
            deny(response, request, null, null, 401, "Not authenticated");
            return false;
        }

        Set<String> allowed = rulesFor(uri);
        if (!allowed.contains(role)) {
            deny(response, request, username, role, 403,
                    "Role " + role + " may not access " + method + " " + uri);
            return false;
        }
        return true;
    }

    private Set<String> rulesFor(String uri) {
        if (uri.startsWith("/api/meta")) return ALL;
        if (uri.startsWith("/api/purchases")) return ALL;
        if (uri.startsWith("/api/transfers")) return ALL;
        if (uri.startsWith("/api/assignments")) return STAFF;
        if (uri.startsWith("/api/expenditures")) return STAFF;
        if (uri.startsWith("/api/dashboard")) return STAFF;
        if (uri.startsWith("/api/audit")) return ADMIN_ONLY;
        return ADMIN_ONLY;
    }

    private void deny(HttpServletResponse response, HttpServletRequest request,
                      String username, String role, int status, String message) throws IOException {
        // denied attempts are part of the audit trail too
        auditService.log(username == null ? "-" : username, role == null ? "-" : role,
                request.getMethod() + " " + request.getRequestURI(), "Access denied: " + message,
                status);
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
