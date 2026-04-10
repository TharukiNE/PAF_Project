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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        User user = userRepository.findById(userResponse.id()).orElseThrow();
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.from(currentUserService.requireCurrentUser()));
    }
}
