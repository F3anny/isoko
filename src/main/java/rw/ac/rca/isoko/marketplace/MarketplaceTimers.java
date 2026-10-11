package rw.ac.rca.isoko.marketplace;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import rw.ac.rca.isoko.order.OrderService;
@Component @EnableScheduling
public class MarketplaceTimers {
    private final OrderService orders;
    public MarketplaceTimers(OrderService orders) {this.orders=orders;}
    @Scheduled(fixedDelay=60000,initialDelay=60000) public void run() {orders.processTimers();}
}
