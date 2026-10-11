package rw.ac.rca.isoko.payment;

import rw.ac.rca.isoko.config.IsokoProperties;
import rw.ac.rca.isoko.order.PaymentProvider;
import rw.ac.rca.isoko.order.PaymentStatus;

import java.util.UUID;

public interface PaymentGateway {
    Result charge(PaymentProvider provider, long amountRwf, UUID orderId, IsokoProperties.StubMode override);

    record Result(PaymentStatus status, String gatewayRef) {}
}
