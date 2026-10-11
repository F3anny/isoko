package rw.ac.rca.isoko.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import rw.ac.rca.isoko.common.exception.ApiException;

import java.util.List;
import java.util.UUID;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public Page<ProductResponse> search(String query, int page, int size) {
        int pageSize = size <= 0 ? 20 : Math.min(size, 20);
        var pageable = PageRequest.of(Math.max(page, 0), pageSize, Sort.by("name"));
        Page<Product> result = products.searchPublished(query == null ? "" : query.trim(), pageable);
        return result
                .map(ProductResponse::publicView);
    }

    public ProductResponse getPublished(UUID id) {
        return products.findByIdAndStatus(id, ProductStatus.PUBLISHED)
                .filter(Product::isPublished)
                .map(ProductResponse::publicView)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    public List<Category> categories() {
        return categories.findAll();
    }
}
