package rw.ac.rca.isoko.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.isoko.catalog.CategoryResponse;
import rw.ac.rca.isoko.catalog.ProductResponse;
import rw.ac.rca.isoko.order.OrderRepository;
import rw.ac.rca.isoko.order.OrderResponse;
import rw.ac.rca.isoko.order.OrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "UC-CS-11 Manage catalogue · UC-CS-12 Fulfill. Requires ROLE_ADMIN.")
public class AdminController {

    private final AdminCatalogService catalog;
    private final OrderService orders;
    private final OrderRepository orderRepository;

    public AdminController(AdminCatalogService catalog, OrderService orders, OrderRepository orderRepository) {
        this.catalog = catalog;
        this.orders = orders;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/products")
    @Operation(summary = "List all products including drafts")
    public List<ProductResponse> products() {
        return catalog.listProducts();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody UpsertProductRequest request) {
        return catalog.create(request);
    }

    @PatchMapping("/products/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertProductRequest request) {
        return catalog.update(id, request);
    }

    @PatchMapping("/products/{id}/stock")
    @Operation(summary = "Set on-hand stock for a product")
    public ProductResponse updateStock(@PathVariable UUID id, @Valid @RequestBody StockUpdateRequest request) {
        return catalog.updateStock(id, request.stockQty());
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalog.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request.name());
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse renameCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return catalog.renameCategory(id, request.name());
    }

    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/orders/{id}/ship")
    @Operation(summary = "Mark a Paid order as Shipped")
    public OrderResponse ship(@PathVariable UUID id, @Valid @RequestBody ShipRequest request) {
        return orders.ship(id, request.trackingNo());
    }

    @PostMapping("/orders/{id}/deliver")
    public OrderResponse deliver(@PathVariable UUID id) {
        return orders.deliver(id);
    }

    @PatchMapping("/orders/{id}")
    @Operation(summary = "Attempt an explicit state change (illegal transitions return 409)")
    public OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request) {
        return orders.forceTransition(id, request.status());
    }
}
