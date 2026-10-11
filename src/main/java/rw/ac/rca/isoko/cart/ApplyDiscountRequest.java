package rw.ac.rca.isoko.cart;

import jakarta.validation.constraints.NotBlank;

public record ApplyDiscountRequest(@NotBlank String code) {}
