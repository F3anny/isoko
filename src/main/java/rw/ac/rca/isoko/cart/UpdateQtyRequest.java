package rw.ac.rca.isoko.cart;

import jakarta.validation.constraints.Min;

public record UpdateQtyRequest(@Min(0) int qty) {}
