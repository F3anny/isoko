package rw.ac.rca.isoko.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final rw.ac.rca.isoko.identity.UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist tokenBlacklist, rw.ac.rca.isoko.identity.UserRepository users) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String jti = claims.getId();
                if (jti != null && !tokenBlacklist.isRevoked(jti) && tokenBlacklist.touchSession(jti)) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    var user = users.findById(userId).orElse(null);
                    if (user == null || user.getStatus() == rw.ac.rca.isoko.identity.UserStatus.DISABLED || user.isLocked(java.time.Instant.now())) {
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }
                    String email = claims.get("email", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> claimedRoles = claims.get("roles", List.class);
                    List<String> roles = user.getRoles().stream().map(Enum::name)
                            .filter(r -> claimedRoles != null && claimedRoles.contains(r)).toList();
                    var authorities = (roles == null ? List.<String>of() : roles).stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .toList();
                    AuthPrincipal principal = new AuthPrincipal(userId, email, authorities);
                    var auth = new UsernamePasswordAuthenticationToken(principal, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("userId", userId.toString());
                    request.setAttribute("jwtJti", jti);
                    request.setAttribute("jwtExp", jwtService.expiry(claims));
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
