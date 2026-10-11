package rw.ac.rca.isoko.seller;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.admin.*;
import rw.ac.rca.isoko.catalog.*;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.identity.*;
import rw.ac.rca.isoko.marketplace.MarketplaceRecords;
import rw.ac.rca.isoko.order.*;
import java.util.*;

@Service @Transactional
public class SellerService {
    private final ShopRepository shops;
    private final ProductRepository products;
    private final AdminCatalogService catalog;
    private final UserRepository users;
    private final OrderRepository orders;
    private final OrderService fulfilment;
    private final MarketplaceRecords records;
    public SellerService(ShopRepository shops,ProductRepository products,AdminCatalogService catalog,
                         UserRepository users,OrderRepository orders,OrderService fulfilment,MarketplaceRecords records) {
        this.shops=shops;this.products=products;this.catalog=catalog;this.users=users;
        this.orders=orders;this.fulfilment=fulfilment;this.records=records;
    }
    public Shop mine(UUID user) { return shops.findByOwnerId(user).orElse(null); }
    public Shop apply(UUID user,String name,String description,String reference) {
        if(mine(user)!=null) throw ApiException.conflict("You already have a shop application.");
        Shop s=new Shop();s.setOwnerId(user);s.setName(name.trim());s.setDescription(description);s.setBusinessReference(reference.trim());
        shops.saveAndFlush(s);records.audit(user,"SHOP_APPLIED",s.getId(),s.getName());
        records.notify(user,"Your shop application is pending manual business verification.");return s;
    }
    public Shop review(UUID actor,UUID id,Shop.Status status,String note) {
        Shop s=shops.findById(id).orElseThrow(()->ApiException.notFound("Shop not found"));
        if(status==Shop.Status.PENDING) throw ApiException.badRequest("Choose approve, reject or suspend.");
        String before=s.getStatus().name();s.setStatus(status);s.setReviewNote(note);
        User owner=users.findById(s.getOwnerId()).orElseThrow();
        if(status==Shop.Status.APPROVED) owner.getRoles().add(Role.SELLER);else owner.getRoles().remove(Role.SELLER);
        records.audit(actor,"SHOP_REVIEWED",id,before+" → "+status+"; "+note);
        records.notify(s.getOwnerId(),"Shop "+s.getName()+": "+status+". "+(note==null?"":note));return s;
    }
    private Shop approved(UUID user) {
        Shop s=mine(user);if(s==null || s.getStatus()!=Shop.Status.APPROVED) throw ApiException.conflict("An approved shop is required.");return s;
    }
    public List<ProductResponse> products(UUID user) {return products.findByShopId(approved(user).getId()).stream().map(ProductResponse::publicView).toList();}
    public ProductResponse create(UUID user,UpsertProductRequest request) {
        Shop s=approved(user);if(request.priceRwf()<=0) throw ApiException.badRequest("Price must be positive.");
        ProductResponse response=catalog.create(request);Product p=products.findById(response.id()).orElseThrow();p.setShop(s);
        records.audit(user,"SELLER_PRODUCT_CREATED",p.getId(),p.getSku());return ProductResponse.publicView(p);
    }
    public ProductResponse update(UUID user,UUID id,UpsertProductRequest request) {
        Shop s=approved(user);Product p=products.findById(id).orElseThrow(()->ApiException.notFound("Product not found"));
        if(p.getShop()==null || !p.getShop().getId().equals(s.getId())) throw ApiException.notFound("Product not found");
        if(request.priceRwf()<=0) throw ApiException.badRequest("Price must be positive.");
        var result=catalog.update(id,request);records.audit(user,"SELLER_PRODUCT_UPDATED",id,request.status().name());return result;
    }
    public List<Map<String,Object>> orders(UUID user) {
        UUID shop=approved(user).getId();List<Map<String,Object>> result=new ArrayList<>();
        for(ShopOrder o:orders.findAll()) {
            var own=o.getLines().stream().filter(l->shop.equals(l.getShopId())).toList();if(own.isEmpty())continue;
            var view=OrderResponse.from(o);Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",o.getId());row.put("orderNumber",o.getOrderNumber());row.put("status",o.getStatus());
            row.put("deliveryMethod",o.getDeliveryMethod());row.put("deliveryAddress",o.getDeliveryAddress());
            row.put("escrowState",o.getEscrowState());row.put("lines",view.lines().stream().filter(l->shop.equals(l.shopId())).toList());
            result.add(row);
        }return result;
    }
    public void ship(UUID user,UUID order,String tracking) {fulfilment.shipShop(order,approved(user).getId(),tracking,false);}
    public List<Map<String,Object>> payouts(UUID user) {return records.payouts(approved(user).getId());}
}
