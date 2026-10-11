package rw.ac.rca.isoko.admin;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(0) int stockQty) {}
