package rw.ac.rca.isoko.marketplace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.isoko.security.AuthPrincipal;
import rw.ac.rca.isoko.seller.*;
import rw.ac.rca.isoko.order.*;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class MarketplaceController {
    private final MarketplaceRecords records;
    private final ShopRepository shops;
    private final SellerService sellers;
    private final OrderService orders;
    public MarketplaceController(MarketplaceRecords records,ShopRepository shops,SellerService sellers,OrderService orders) {
        this.records=records;this.shops=shops;this.sellers=sellers;this.orders=orders;
    }
    public record Review(@NotNull Shop.Status status,@Size(max=2000) String note) {}
    public record Delivery(@NotNull UUID lineId,@NotBlank @Pattern(regexp="[0-9]{6}") String otp) {}
    public record Dispute(@NotBlank @Size(max=2000) String reason,@Size(max=500) String evidence) {}
    public record Resolution(boolean refund,@NotBlank @Size(max=2000) String note) {}
    @GetMapping("/notifications") public List<Map<String,Object>> notifications(@AuthenticationPrincipal AuthPrincipal p) {return records.notifications(p.userId());}
    @GetMapping("/admin/shops") public List<Shop> shops() {return shops.findAll();}
    @PatchMapping("/admin/shops/{id}") public Shop review(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody Review r) {return sellers.review(p.userId(),id,r.status(),r.note());}
    @PostMapping("/orders/{id}/cancel") public OrderResponse cancel(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id) {return orders.cancel(p.userId(),id);}
    @PostMapping("/orders/{id}/confirm-delivery") public OrderResponse confirm(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody Delivery r) {return orders.confirmDelivery(p.userId(),id,r.lineId(),r.otp());}
    @PostMapping("/orders/{id}/dispute") public OrderResponse dispute(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody Dispute r) {return orders.dispute(p.userId(),id,r.reason(),r.evidence());}
    @PostMapping("/admin/orders/{id}/resolve") public OrderResponse resolve(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody Resolution r) {return orders.resolve(p.userId(),id,r.refund(),r.note());}
    @GetMapping("/admin/ledger") public List<Map<String,Object>> ledger() {return records.ledger();}
    @GetMapping("/admin/audit") public List<Map<String,Object>> audit() {return records.audit();}
    public record Fees(@Min(0) @Max(50) int commissionPercent,@Min(0) @Max(50) int paymentFeePercent) {}
    @GetMapping("/admin/settings") public Map<String,Object> settings() {return records.settings();}
    @PatchMapping("/admin/settings") public void settings(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody Fees r) {records.settings(p.userId(),r.commissionPercent(),r.paymentFeePercent());}
}
