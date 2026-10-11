package rw.ac.rca.isoko.commerce;

import org.junit.jupiter.api.Test;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.identity.LoyaltyTier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingTest {

    @Test void isokoDeliveryAndDiscountBoundaries() {
        assertEquals(1_000, Pricing.shippingRwf(49_999, LoyaltyTier.STANDARD, "PICKUP"));
        assertEquals(2_500, Pricing.shippingRwf(49_999, LoyaltyTier.STANDARD, "HOME_KIGALI"));
        assertEquals(4_000, Pricing.shippingRwf(49_999, LoyaltyTier.STANDARD, "HOME_OTHER"));
        assertEquals(0, Pricing.shippingRwf(50_000, LoyaltyTier.STANDARD, "HOME_OTHER"));
        assertEquals(0, Pricing.shippingRwf(10_000, LoyaltyTier.GOLD, "PICKUP"));
        assertEquals(5_000, Pricing.discountRwf(10_000, 1, 90));
    }

    @Test
    void appliesTenPercentFromFiveItems() {
        assertEquals(450, Pricing.calculateTotal(5, 100));
    }

    @Test
    void noDiscountBelowFiveItems() {
        assertEquals(400, Pricing.calculateTotal(4, 100));
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> Pricing.calculateTotal(-1, 100));
    }

    @Test
    void codeAndQuantityDoNotStackBeyondMax() {
        long subtotal = 5 * 15_000;
        long discount = Pricing.discountRwf(subtotal, 5, 10);
        assertEquals(subtotal * 10 / 100, discount);
    }

    @Test
    void goldGetsFreeShippingUnderThreshold() {
        assertEquals(0, Pricing.shippingRwf(30_000, LoyaltyTier.GOLD));
        assertEquals(2_500, Pricing.shippingRwf(30_000, LoyaltyTier.STANDARD));
        assertEquals(0, Pricing.shippingRwf(50_000, LoyaltyTier.STANDARD));
    }

    @Test
    void discountCodeBoundaries() {
        Pricing.assertCodeFormat("SAVE2");
        Pricing.assertCodeFormat("SAVE2026");
        assertThrows(ApiException.class, () -> Pricing.assertCodeFormat("AB12"));
        assertThrows(ApiException.class, () -> Pricing.assertCodeFormat("SAVE2026SUMMER"));
        assertThrows(ApiException.class, () -> Pricing.assertCodeFormat("SAVE-25!"));
    }
}
