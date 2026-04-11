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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * AuthController exposes endpoints for authentication operations:
 * registration, login, demo auto-login, and retrieving current user.
 */
@Tag(name = "Authentication", description = "User authentication (register, login, auto-login, get current user)")
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
     * Registers a new user account and returns an authentication response with JWT token.
     * This creates the user record, issues a JWT, and returns the logged-in user in AuthResponse.
     */
    @Operation(
            summary = "Register new user account",
            description = "Creates a new user account with email and password. Returns JWT token and user details for immediate login. " +
                    "User starts with default STUDENT role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User account created successfully with JWT token"),
            @ApiResponse(responseCode = "400", description = "Invalid input: email already exists, invalid email format, or weak password"),
            @ApiResponse(responseCode = "409", description = "User with this email already registered")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        User user = userRepository.findById(userResponse.id()).orElseThrow();
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), userResponse);
    }

    /**
     * Authenticates an existing user with email and password credentials.
     * On success, returns a JWT plus the authenticated user's details for session initialization.
     */
    @Operation(
            summary = "User login with email and password",
            description = "Authenticates user credentials and returns JWT access token. Token expires after configured duration. " +
                    "Include token in Authorization header as 'Bearer <token>' for subsequent requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful with JWT token and user details"),
            @ApiResponse(responseCode = "400", description = "Malformed request or missing credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account disabled or locked")
    })
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
     * Returns a demo user account for quick access in development/testing.
     * The demo user is created if it does not already exist.
     */
    @Operation(
            summary = "Auto-login with demo account",
            description = "Initiates login with a pre-configured demo account for testing. Useful for quick onboarding or demos. " +
                    "Creates demo user if it doesn't exist. Returns JWT and user details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demo login successful with JWT token"),
            @ApiResponse(responseCode = "500", description = "Demo user creation failed")
    })
    @PostMapping("/auto-login")
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
     * Requires valid JWT token in Authorization header.
     */
    @Operation(
            summary = "Get current authenticated user",
            description = "Returns the profile of the currently authenticated user. Use this endpoint to validate JWT token " +
                    "and get fresh user details after login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "User account disabled or token expired")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.from(currentUserService.requireCurrentUser()));
    }
}
