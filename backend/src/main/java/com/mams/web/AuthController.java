package com.mams.web;

import com.mams.dto.LoginRequest;
import com.mams.dto.SessionUser;
import com.mams.model.User;
import com.mams.repository.UserRepository;
import com.mams.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final AuditService audit;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        User user = users.findByUsername(request.username());
        if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            audit.log(request.username() == null ? "-" : request.username(), "-",
                    "POST /api/auth/login", "Failed login attempt", 401);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid username or password");
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().name());
        session.setAttribute("baseId", user.getBase() == null ? null : user.getBase().getId());

        audit.log(user.getUsername(), user.getRole().name(), "POST /api/auth/login",
                "Signed in", 200);

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "baseId", user.getBase() == null ? "" : user.getBase().getId(),
                "baseName", user.getBase() == null ? "" : user.getBase().getName()));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        SessionUser user = SessionUser.from(request);
        if (user != null) {
            audit.log(user, "POST /api/auth/logout", "Signed out", 200);
        }
        request.getSession().invalidate();
        return Map.of("status", "signed out");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        SessionUser user = SessionUser.from(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(Map.of(
                "username", user.username(),
                "role", user.role(),
                "baseId", user.baseId() == null ? "" : user.baseId()));
    }
}
