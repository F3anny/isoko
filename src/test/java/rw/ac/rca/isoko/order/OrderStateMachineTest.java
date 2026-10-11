package rw.ac.rca.isoko.order;

import org.junit.jupiter.api.Test;
import rw.ac.rca.isoko.common.exception.ApiException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {

    @Test
    void legalHappyPath() {
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(OrderStatus.PLACED, OrderStatus.PAID));
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(OrderStatus.PAID, OrderStatus.SHIPPED));
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void rejectsDeliveredToPlaced() {
        assertThrows(ApiException.class,
                () -> OrderStateMachine.assertTransition(OrderStatus.DELIVERED, OrderStatus.PLACED));
    }

    @Test
    void rejectsPaidToPlaced() {
        assertThrows(ApiException.class,
                () -> OrderStateMachine.assertTransition(OrderStatus.PAID, OrderStatus.PLACED));
    }

    @Test
    void timeoutThenPayIsLegal() {
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(OrderStatus.PAYMENT_TIMED_OUT, OrderStatus.PAID));
    }
}
