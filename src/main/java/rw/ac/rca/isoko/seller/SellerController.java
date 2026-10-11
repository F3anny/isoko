package rw.ac.rca.isoko.seller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.isoko.security.AuthPrincipal;
import rw.ac.rca.isoko.admin.UpsertProductRequest;
import rw.ac.rca.isoko.admin.ShipRequest;
import rw.ac.rca.isoko.catalog.ProductResponse;
import java.util.*;

@RestController @RequestMapping("/api/v1/seller")
public class SellerController {
    private final SellerService sellers;
    public SellerController(SellerService sellers) {this.sellers=sellers;}
    public record Application(@NotBlank @Size(max=120) String name,@Size(max=2000) String description,@NotBlank @Size(max=120) String businessReference) {}
    @GetMapping("/shop") public Shop shop(@AuthenticationPrincipal AuthPrincipal p) {return sellers.mine(p.userId());}
    @PostMapping("/shop") public Shop apply(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody Application r) {return sellers.apply(p.userId(),r.name(),r.description(),r.businessReference());}
    @GetMapping("/products") public List<ProductResponse> products(@AuthenticationPrincipal AuthPrincipal p) {return sellers.products(p.userId());}
    @PostMapping("/products") public ProductResponse create(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody UpsertProductRequest r) {return sellers.create(p.userId(),r);}
    @PatchMapping("/products/{id}") public ProductResponse update(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody UpsertProductRequest r) {return sellers.update(p.userId(),id,r);}
    @GetMapping("/orders") public List<Map<String,Object>> orders(@AuthenticationPrincipal AuthPrincipal p) {return sellers.orders(p.userId());}
    @PostMapping("/orders/{id}/ship") public void ship(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody ShipRequest r) {sellers.ship(p.userId(),id,r.trackingNo());}
    @GetMapping("/payouts") public List<Map<String,Object>> payouts(@AuthenticationPrincipal AuthPrincipal p) {return sellers.payouts(p.userId());}
}
