package com.sliit.smartcampus.security;

import com.sliit.smartcampus.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Unified principal for both form-login and Google OAuth2 sessions.
 * Implements both UserDetails and OAuth2User so Spring Security treats
 * both login paths identically when building the authentication object.
 */
@Getter
public class CampusUserDetails implements UserDetails, OAuth2User, Serializable {

    private static final long serialVersionUID = 1L;

    private final User domainUser;
    private final List<GrantedAuthority> authorities;
    private Map<String, Object> attributes = Map.of();

    public CampusUserDetails(User domainUser) {
        this.domainUser = domainUser;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + domainUser.getRole().name()));
    }

    /** Called after Google OAuth2 to attach the raw Google attributes. */
    public CampusUserDetails withAttributes(Map<String, Object> attrs) {
        this.attributes = Map.copyOf(attrs);
        return this;
    }

    // ── OAuth2User ────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** OAuth2User.getName() — Spring uses this as the principal name. */
    @Override
    public String getName() {
        return domainUser.getEmail();
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return domainUser.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return domainUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        boolean hasPassword = domainUser.getPasswordHash() != null && !domainUser.getPasswordHash().isBlank();
        boolean hasGoogle   = domainUser.getGoogleId()    != null && !domainUser.getGoogleId().isBlank();
        return hasPassword || hasGoogle;
    }
}
