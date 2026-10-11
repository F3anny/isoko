package rw.ac.rca.isoko.admin;

import jakarta.validation.constraints.NotNull;
import rw.ac.rca.isoko.order.OrderStatus;

public record StatusChangeRequest(@NotNull OrderStatus status) {}
