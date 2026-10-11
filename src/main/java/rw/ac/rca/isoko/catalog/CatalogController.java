package rw.ac.rca.isoko.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalogue", description = "UC-CS-01 Search · UC-CS-02 View product")
@SecurityRequirements
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    @Operation(summary = "List/search published products (page size capped at 20)")
    public Page<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.search(q, page, size);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Product detail. Unpublished or unknown ids return 404.")
    public ProductResponse one(@PathVariable UUID id) {
        return catalog.getPublished(id);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalog.categories().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }
}
