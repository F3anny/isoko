package rw.ac.rca.isoko.commerce;

import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.identity.LoyaltyTier;

/**
 * Pure pricing rules from SRS Appendix A / FR-DISC / FR-SHIP.
 * Quantity discount is 10% at qty >= 5. A promotional code is also a percent-off.
 * Combined discount is the maximum of the two so they never stack to 20%.
 */
public final class Pricing {

    public static final int QTY_DISCOUNT_THRESHOLD = 5;
    public static final int QTY_DISCOUNT_PERCENT = 10;
    public static final long FREE_SHIPPING_THRESHOLD_RWF = 50_000;
    public static final long STANDARD_SHIPPING_RWF = 2_500;
    public static final String CODE_PATTERN = "^[A-Za-z0-9]{5,10}$";

    private Pricing() {}

    public static void assertCodeFormat(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Discount code is required.");
        }
        String trimmed = code.trim();
        if (trimmed.length() < 5 || trimmed.length() > 10) {
            throw ApiException.badRequest("code must be 5–10 characters");
        }
        if (!trimmed.matches("^[A-Za-z0-9]+$")) {
            throw ApiException.badRequest("code contains invalid characters");
        }
    }

    public static int quantityDiscountPercent(int totalQty) {
        return totalQty >= QTY_DISCOUNT_THRESHOLD ? QTY_DISCOUNT_PERCENT : 0;
    }

    public static long discountRwf(long subtotal, int totalQty, Integer codePercent) {
        int qtyPct = quantityDiscountPercent(totalQty);
        int codePct = codePercent == null ? 0 : codePercent;
        int applied = Math.min(50, Math.max(qtyPct, codePct));
        return subtotal * applied / 100;
    }

    public static long shippingRwf(long subtotalAfterDiscount, LoyaltyTier tier) {
        if (tier == LoyaltyTier.GOLD || subtotalAfterDiscount >= FREE_SHIPPING_THRESHOLD_RWF) {
            return 0;
        }
        return STANDARD_SHIPPING_RWF;
    }

    public static long shippingRwf(long subtotal, LoyaltyTier tier, String method) {
        if (tier == LoyaltyTier.GOLD || subtotal >= FREE_SHIPPING_THRESHOLD_RWF) return 0;
        return switch (method) {
            case "PICKUP" -> 1_000;
            case "HOME_KIGALI" -> 2_500;
            case "HOME_OTHER" -> 4_000;
            default -> throw ApiException.badRequest("Choose a valid delivery method.");
        };
    }

    /** TDD example from the lecture notes. */
    public static long calculateTotal(int quantity, long unitPrice) {
        if (quantity < 0 || unitPrice < 0) {
            throw new IllegalArgumentException("quantity and unitPrice must be non-negative");
        }
        long subtotal = quantity * unitPrice;
        return quantity >= QTY_DISCOUNT_THRESHOLD ? subtotal * 90 / 100 : subtotal;
    }
}
