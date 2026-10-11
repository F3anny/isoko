package rw.ac.rca.isoko.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.catalog.Category;
import rw.ac.rca.isoko.catalog.CategoryRepository;
import rw.ac.rca.isoko.catalog.CategoryResponse;
import rw.ac.rca.isoko.catalog.Product;
import rw.ac.rca.isoko.catalog.ProductRepository;
import rw.ac.rca.isoko.catalog.ProductResponse;
import rw.ac.rca.isoko.common.exception.ApiException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminCatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final rw.ac.rca.isoko.marketplace.MarketplaceRecords records;

    public AdminCatalogService(ProductRepository products, CategoryRepository categories, rw.ac.rca.isoko.marketplace.MarketplaceRecords records) {
        this.products = products;
        this.categories = categories;
        this.records = records;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return products.findAll(Sort.by("name")).stream().map(ProductResponse::publicView).toList();
    }

    @Transactional
    public ProductResponse create(UpsertProductRequest request) {
        if (products.existsBySkuIgnoreCase(request.sku())) {
            throw ApiException.conflict("SKU already exists.");
        }
        Product product = new Product();
        apply(product, request, true);
        products.saveAndFlush(product);
        records.audit(null,"PRODUCT_CREATED",product.getId(),snapshot(product));
        return ProductResponse.publicView(products.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, UpsertProductRequest request) {
        Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        String before=snapshot(product);
        apply(product, request, false);
        records.audit(null,"PRODUCT_UPDATED",id,before+" → "+snapshot(product));
        return ProductResponse.publicView(products.save(product));
    }

    @Transactional
    public ProductResponse updateStock(UUID id, int stockQty) {
        Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        int before=product.getStockQty();
        product.setStockQty(stockQty);
        records.audit(null,"PRODUCT_STOCK_UPDATED",id,before+" → "+stockQty);
        return ProductResponse.publicView(products.save(product));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAll(Sort.by("name")).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(String name) {
        String trimmed = name.trim();
        if (categories.findByNameIgnoreCase(trimmed).isPresent()) {
            throw ApiException.conflict("A category with that name already exists.");
        }
        Category category=categories.saveAndFlush(new Category(trimmed));
        records.audit(null,"CATEGORY_CREATED",category.getId(),trimmed);
        return new CategoryResponse(category.getId(), trimmed);
    }

    @Transactional
    public CategoryResponse renameCategory(UUID id, String name) {
        Category category = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
        String trimmed = name.trim();
        categories.findByNameIgnoreCase(trimmed)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw ApiException.conflict("A category with that name already exists.");
                });
        String before=category.getName();category.setName(trimmed);
        records.audit(null,"CATEGORY_RENAMED",id,before+" → "+trimmed);
        return new CategoryResponse(categories.save(category).getId(), trimmed);
    }

    private void apply(Product product, UpsertProductRequest request, boolean creating) {
        if (creating) {
            product.setSku(request.sku().trim().toUpperCase());
        }
        if (request.stockQty() < 0) {
            throw ApiException.badRequest("stockQty cannot be negative.");
        }
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPriceRwf(request.priceRwf());
        product.setStockQty(request.stockQty());
        product.setStatus(request.status());
        if (request.category() != null && !request.category().isBlank()) {
            Category category = categories.findByNameIgnoreCase(request.category().trim())
                    .orElseGet(() -> categories.save(new Category(request.category().trim())));
            product.setCategory(category);
        }
        if (request.imageUrl() != null) {
            String url = request.imageUrl().trim();
            if (url.length() > 400_000) {
                throw ApiException.badRequest("Photo is too large. Use a smaller JPEG.");
            }
            if (!url.isEmpty() && !url.startsWith("data:image/") && !url.startsWith("/products/")) {
                throw ApiException.badRequest("Unsupported photo.");
            }
            product.setImageUrl(url.isEmpty() ? null : url);
        }
    }

    private static String snapshot(Product p) {
        return p.getSku()+"; name="+p.getName()+"; price="+p.getPriceRwf()+"; stock="+p.getStockQty()+"; status="+p.getStatus();
    }
}
