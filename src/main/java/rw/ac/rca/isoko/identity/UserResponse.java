package rw.ac.rca.isoko.identity;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        UserStatus status,
        LoyaltyTier loyaltyTier,
        Set<Role> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getLoyaltyTier(),
                user.getRoles()
        );
    }
}
