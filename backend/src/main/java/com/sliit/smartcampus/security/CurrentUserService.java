package com.sliit.smartcampus.security;

import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.exception.ApiException;
import com.sliit.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (auth.getPrincipal() instanceof CampusUserDetails details) {
            return userRepository.findById(details.getDomainUser().getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Unsupported principal");
    }
}
