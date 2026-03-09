package org.mangala.wallet.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mangala.security.SecurityConstants;
import org.mangala.security.preauthenticated.PreAuthenticatedPrincipal;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filter that authenticates requests using pre-authenticated headers from upstream systems.
 *
 * This service trusts that authentication has already been performed by the API gateway,
 * which sets headers like X-User-Id, X-User-Email, X-User-Roles, X-User-Permissions.
 */
public class PreAuthenticatedHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(SecurityConstants.HEADER_USER_ID);

        if (StringUtils.hasText(userId)) {
            String email = request.getHeader(SecurityConstants.HEADER_USER_EMAIL);
            String rolesHeader = request.getHeader(SecurityConstants.HEADER_USER_ROLES);
            String permissionsHeader = request.getHeader(SecurityConstants.HEADER_USER_PERMISSIONS);

            Set<String> roles = parseCommaSeparated(rolesHeader);
            Set<String> permissions = parseCommaSeparated(permissionsHeader);

            PreAuthenticatedPrincipal principal = PreAuthenticatedPrincipal.builder()
                    .userId(userId)
                    .email(email)
                    .roles(roles)
                    .permissions(permissions)
                    .build();

            PreAuthenticatedToken authentication = new PreAuthenticatedToken(principal, roles, permissions);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private Set<String> parseCommaSeparated(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptySet();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Spring Security authentication token for pre-authenticated requests.
     */
    public static class PreAuthenticatedToken extends AbstractAuthenticationToken {

        private final PreAuthenticatedPrincipal principal;

        public PreAuthenticatedToken(PreAuthenticatedPrincipal principal, Set<String> roles, Set<String> permissions) {
            super(buildAuthorities(roles, permissions));
            this.principal = principal;
            setAuthenticated(true);
        }

        private static Collection<GrantedAuthority> buildAuthorities(Set<String> roles, Set<String> permissions) {
            return Stream.concat(
                    roles.stream().map(SimpleGrantedAuthority::new),
                    permissions.stream().map(perm -> new SimpleGrantedAuthority("PERMISSION_" + perm))
            ).collect(Collectors.toSet());
        }

        @Override
        public Object getCredentials() {
            return null; // No credentials - upstream already validated
        }

        @Override
        public PreAuthenticatedPrincipal getPrincipal() {
            return principal;
        }
    }
}
