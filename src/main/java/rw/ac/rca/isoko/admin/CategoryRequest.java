package rw.ac.rca.isoko.admin;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name) {}
