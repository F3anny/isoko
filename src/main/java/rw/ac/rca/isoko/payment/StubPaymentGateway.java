package rw.ac.rca.isoko.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rw.ac.rca.isoko.config.IsokoProperties;
import rw.ac.rca.isoko.order.PaymentProvider;
import rw.ac.rca.isoko.order.PaymentStatus;

import java.util.UUID;

/**
 * Controllable stub. Gateway internals are out of scope (SRS §1.2).
 * TIMEOUT is returned immediately so UAT does not wait 45 seconds; the order is still
 * marked PAYMENT_TIMED_OUT and is never Paid.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);
    private final IsokoProperties properties;

    public StubPaymentGateway(IsokoProperties properties) {
        this.properties = properties;
    }

    @Override
    public Result charge(PaymentProvider provider, long amountRwf, UUID orderId, IsokoProperties.StubMode override) {
        IsokoProperties.StubMode mode = override != null ? override : properties.payment().stubMode();
        log.info("payment_stub provider={} amountRwf={} orderId={} mode={}", provider, amountRwf, orderId, mode);
        return switch (mode) {
            case SUCCESS -> new Result(PaymentStatus.SUCCEEDED, "stub_" + provider.name() + "_" + orderId.toString().substring(0, 8));
            case DECLINE -> new Result(PaymentStatus.DECLINED, null);
            case TIMEOUT -> new Result(PaymentStatus.TIMED_OUT, null);
        };
    }
}
