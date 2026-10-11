package rw.ac.rca.isoko.admin;

import jakarta.validation.constraints.NotBlank;

public record ShipRequest(@NotBlank String trackingNo) {}
