package rw.ac.rca.isoko.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.catalog.Category;
import rw.ac.rca.isoko.catalog.CategoryRepository;
import rw.ac.rca.isoko.catalog.Product;
import rw.ac.rca.isoko.catalog.ProductRepository;
import rw.ac.rca.isoko.catalog.ProductStatus;
import rw.ac.rca.isoko.commerce.DiscountCode;
import rw.ac.rca.isoko.commerce.DiscountCodeRepository;
import rw.ac.rca.isoko.identity.LoyaltyTier;
import rw.ac.rca.isoko.identity.Role;
import rw.ac.rca.isoko.identity.User;
import rw.ac.rca.isoko.identity.UserRepository;
import rw.ac.rca.isoko.identity.UserStatus;

import java.time.Instant;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "IsokoDemo1";
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final IsokoProperties properties;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final DiscountCodeRepository discountCodes;
    private final rw.ac.rca.isoko.seller.ShopRepository shops;

    public DataInitializer(IsokoProperties properties, UserRepository users, PasswordEncoder encoder,
                           CategoryRepository categories, ProductRepository products,
                           DiscountCodeRepository discountCodes, rw.ac.rca.isoko.seller.ShopRepository shops) {
        this.properties = properties;
        this.users = users;
        this.encoder = encoder;
        this.categories = categories;
        this.products = products;
        this.discountCodes = discountCodes;
        this.shops = shops;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seed().enabled()) {
            return;
        }
        seedUser("demo@isoko.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        seedUser("gold@isoko.rw", LoyaltyTier.GOLD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        User locked = seedUser("locked@isoko.rw", LoyaltyTier.STANDARD, UserStatus.LOCKED, Set.of(Role.CUSTOMER));
        locked.setLockedUntil(Instant.now().plusSeconds(3600));
        users.save(locked);
        seedUser("ops@isoko.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.ADMIN));

        Category cables = categories.findByNameIgnoreCase("Cables")
                .orElseGet(() -> categories.save(new Category("Cables")));
        seedProduct("USB-C-01", "USB-C Cable", 15_000, 20, ProductStatus.PUBLISHED, cables,
                "A Isoko catalogue item. Money is integer RWF.", null);
        seedProduct("OOS-01", "Out of stock demo", 5_000, 0, ProductStatus.PUBLISHED, cables,
                "Used in UAT for out-of-stock behaviour.", null);
        seedProduct("HIDDEN-01", "Unpublished draft", 9_999, 10, ProductStatus.DRAFT, cables, null, null);

        Category phones = categories.findByNameIgnoreCase("Phones")
                .orElseGet(() -> categories.save(new Category("Phones")));
        seedProduct("PHN-01", "Isoko Phone Silver", 185_000, 8, ProductStatus.PUBLISHED, phones,
                "Light studio silver handset. Integer RWF, 10% off at qty 5+.", "/products/phone-01.jpg");
        seedProduct("PHN-02", "Isoko Phone Graphite", 245_000, 6, ProductStatus.PUBLISHED, phones,
                "Graphite finish, catalogue studio lighting.", "/products/phone-02.jpg");
        seedProduct("PHN-03", "Isoko Phone Sky", 165_000, 10, ProductStatus.PUBLISHED, phones,
                "Sky-blue accent phone on a pale backdrop.", "/products/phone-03.jpg");
        seedProduct("PHN-04", "Isoko Phone Champagne", 210_000, 7, ProductStatus.PUBLISHED, phones,
                "Champagne gold, compressed studio photo.", "/products/phone-04.jpg");

        if (discountCodes.findByCodeIgnoreCase("SAVE2026").isEmpty()) {
            DiscountCode code = new DiscountCode();
            code.setCode("SAVE2026");
            code.setPercentOff(10);
            code.setMinQty(1);
            code.setActive(true);
            discountCodes.save(code);
        }
        seedUser("amina@isoko.test", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        seedUser("gold@isoko.test", LoyaltyTier.GOLD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        User uatLocked=seedUser("locked@isoko.test", LoyaltyTier.STANDARD, UserStatus.LOCKED, Set.of(Role.CUSTOMER));
        if (uatLocked.getLockedUntil()==null) uatLocked.setLockedUntil(Instant.now().plusSeconds(1800));
        User seller = seedUser("eric.seller@isoko.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER, Role.SELLER));
        var shop = seedShop(seller, "Eric Fashion", rw.ac.rca.isoko.seller.Shop.Status.APPROVED);
        User pending = seedUser("books.seller@isoko.rw", LoyaltyTier.STANDARD, UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
        var books = seedShop(pending, "Kigali Books", rw.ac.rca.isoko.seller.Shop.Status.PENDING);
        Category fashion = categories.findByNameIgnoreCase("Fashion").orElseGet(() -> categories.save(new Category("Fashion")));
        seedProduct("TS-001", "T-shirt", 8_000, 20, ProductStatus.PUBLISHED, fashion, "Everyday cotton T-shirt from Eric Fashion.", null);
        seedProduct("PH-500", "Phone case", 5_000, 100, ProductStatus.PUBLISHED, phones, "Protective phone case from Eric Fashion.", null);
        for (String sku : java.util.List.of("TS-001", "PH-500")) { var product = products.findBySkuIgnoreCase(sku).orElseThrow(); if (product.getShop() == null) product.setShop(shop); }
        seedProduct("PC-500", "Phone case (UAT)", 5_000, 100, ProductStatus.PUBLISHED, phones, "UAT phone case: 5 items total 22,500 RWF after discount.", null);
        var uatCase=products.findBySkuIgnoreCase("PC-500").orElseThrow();if(uatCase.getShop()==null)uatCase.setShop(shop);
        seedProduct("LIM-001", "Limited stock item", 6_000, 1, ProductStatus.PUBLISHED, fashion, "Single unit for concurrent checkout tests.", null);
        Category bookCategory = categories.findByNameIgnoreCase("Books").orElseGet(() -> categories.save(new Category("Books")));
        seedProduct("BK-010", "Book", 12_500, 0, ProductStatus.PUBLISHED, bookCategory, "Pending KYC shop: hidden from public catalogue.", null);
        seedProduct("BK-020", "Book (in stock)", 12_500, 20, ProductStatus.PUBLISHED, bookCategory, "Approve Kigali Books before purchasing this item.", null);
        var inStockBook=products.findBySkuIgnoreCase("BK-020").orElseThrow();if(inStockBook.getShop()==null)inStockBook.setShop(books);
        var book = products.findBySkuIgnoreCase("BK-010").orElseThrow(); if (book.getShop() == null) book.setShop(books);
        for (String codeName : java.util.List.of("SAVE10", "SUMMER2026", "EXPIRED1")) {
            if (discountCodes.findByCodeIgnoreCase(codeName).isEmpty()) {
                DiscountCode code = new DiscountCode(); code.setCode(codeName); code.setPercentOff(10); code.setMinQty(1); code.setActive(!codeName.equals("EXPIRED1")); discountCodes.save(code);
            }
        }
        log.info("Demo catalogue and accounts are ready (password length {})", DEMO_PASSWORD.length());
    }

    private rw.ac.rca.isoko.seller.Shop seedShop(User user, String name, rw.ac.rca.isoko.seller.Shop.Status status) {
        return shops.findByOwnerId(user.getId()).orElseGet(() -> {
            var s = new rw.ac.rca.isoko.seller.Shop(); s.setOwnerId(user.getId()); s.setName(name); s.setBusinessReference("LAB-" + name.replace(" ", "-")); s.setStatus(status); return shops.save(s);
        });
    }

    private User seedUser(String email, LoyaltyTier tier, UserStatus status, Set<Role> roles) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(encoder.encode(DEMO_PASSWORD));
            user.setPhone("078" + String.format("%07d", Math.floorMod(email.hashCode(), 10_000_000)));
            user.setPhoneVerified(true);
            user.setLoyaltyTier(tier);
            user.setStatus(status);
            user.setRoles(new java.util.HashSet<>(roles));
            return users.save(user);
        });
    }

    private void seedProduct(String sku, String name, long price, int stock, ProductStatus status, Category category,
                             String description, String imageUrl) {
        if (products.findBySkuIgnoreCase(sku).isPresent()) {
            return;
        }
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPriceRwf(price);
        product.setStockQty(stock);
        product.setStatus(status);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        products.save(product);
    }
}
