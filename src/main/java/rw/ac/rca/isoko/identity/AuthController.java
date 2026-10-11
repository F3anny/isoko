package rw.ac.rca.isoko.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.security.AuthPrincipal;
import rw.ac.rca.isoko.security.TokenBlacklist;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "UC-CS-03 Register · UC-CS-04 Login")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;
    private final TokenBlacklist blacklist;

    public AuthController(AuthService authService, UserRepository users, TokenBlacklist blacklist) {
        this.authService = authService;
        this.users = users;
        this.blacklist = blacklist;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Create a customer account and sign in")
    public TokenResponse register(@Valid @RequestBody RegisterRequest body,
                                  @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return authService.register(body, cartToken);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Sign in. Errors are generic (no user enumeration).")
    public TokenResponse login(@Valid @RequestBody LoginRequest body,
                               @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return authService.login(body, cartToken);
    }

    public record Verification(@jakarta.validation.constraints.NotNull java.util.UUID challengeId,
                               @jakarta.validation.constraints.Pattern(regexp="[0-9]{6}") @jakarta.validation.constraints.NotBlank String code) {}
    public record Resend(@jakarta.validation.constraints.NotNull java.util.UUID challengeId) {}
    @PostMapping("/verify") public TokenResponse verify(@Valid @RequestBody Verification r) { return authService.verify(r.challengeId(),r.code()); }
    @PostMapping("/resend") public TokenResponse resend(@Valid @RequestBody Resend r) { return authService.resend(r.challengeId()); }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current access token")
    public Map<String, String> logout(HttpServletRequest request) {
        Object jti = request.getAttribute("jwtJti");
        Object exp = request.getAttribute("jwtExp");
        if (jti instanceof String id && exp instanceof Instant expiresAt) {
            blacklist.revoke(id, expiresAt);
        }
        return Map.of("status", "signed-out");
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return users.findById(principal.userId())
                .map(u -> new UserResponse(u.getId(),u.getEmail(),u.getPhone(),u.getStatus(),u.getLoyaltyTier(),
                        u.getRoles().stream().filter(r -> principal.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_"+r.name()))).collect(java.util.stream.Collectors.toSet())))
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
