package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.user.AuthResponse;
import com.sliit.smartcampus.dto.user.LoginRequest;
import com.sliit.smartcampus.dto.user.RegisterRequest;
import com.sliit.smartcampus.dto.user.UserResponse;
import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.repository.UserRepository;
import com.sliit.smartcampus.security.CampusUserDetails;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.security.JwtService;
import com.sliit.smartcampus.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * AuthController exposes endpoints for registration, login, demo auto-login,
 * and retrieving the current authenticated user.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Registers a new user account and returns an authentication response.
     * This creates the user record, issues a JWT, and returns the logged-in user.
     */
    @PostMapping("/register")
    @Operation(security = {})
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        User user = userRepository.findById(userResponse.id()).orElseThrow();
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), userResponse);
    }

    /**
     * Authenticates an existing user with email and password.
     * On success, returns a JWT plus the authenticated user's details.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password() == null ? "" : request.password();
        UsernamePasswordAuthenticationToken authReq =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManager.authenticate(authReq);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        CampusUserDetails details = (CampusUserDetails) authentication.getPrincipal();
        User user = details.getDomainUser();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.of(token, jwtService.getExpirationSeconds(), UserResponse.from(user)));
    }

    /**
     * Returns a demo user account for quick access in development.
     * The demo user is created if it does not already exist.
     */
    @PostMapping("/auto-login")
    @Operation(security = {})
    public ResponseEntity<AuthResponse> autoLogin(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        User demo = authService.ensureAutoLoginUser();
        CampusUserDetails details = new CampusUserDetails(demo);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        String token = jwtService.generateToken(demo);
        return ResponseEntity.ok(AuthResponse.of(token, jwtService.getExpirationSeconds(), UserResponse.from(demo)));
    }

    /**
     * Returns the currently authenticated user's public profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.from(currentUserService.requireCurrentUser()));
    }
}
