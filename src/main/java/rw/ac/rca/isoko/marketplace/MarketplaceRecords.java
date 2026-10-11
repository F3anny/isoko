package rw.ac.rca.isoko.marketplace;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Durable lab adapters: in-app notifications and balanced, append-only money movements. */
@Service
public class MarketplaceRecords {
    private final JdbcTemplate jdbc;
    public MarketplaceRecords(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void notify(UUID user, String message) {
        jdbc.update("INSERT INTO cloud_sphere.notifications(id,user_id,message) VALUES (?,?,?)", UUID.randomUUID(),user,message);
    }
    public void lockCheckoutKey(String key) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
                statement.setString(1,key); statement.execute();
            }
            return null;
        });
    }
    public void audit(UUID actor, String action, UUID subject, String details) {
        if (actor == null) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof rw.ac.rca.isoko.security.AuthPrincipal p) actor = p.userId();
        }
        jdbc.update("INSERT INTO cloud_sphere.audit_events(id,actor_id,action,subject_id,details) VALUES (?,?,?,?,?)",
                UUID.randomUUID(),actor,action,subject,details);
    }
    public void move(UUID order, String key, String debit, String credit, long amount) {
        if (amount <= 0) return;
        jdbc.update("INSERT INTO cloud_sphere.ledger_movements(id,order_id,movement_key,debit_account,credit_account,amount_rwf) VALUES (?,?,?,?,?,?) ON CONFLICT (movement_key) DO NOTHING",
                UUID.randomUUID(),order,key,debit,credit,amount);
    }
    public List<Map<String,Object>> notifications(UUID user) {
        return jdbc.queryForList("SELECT id,message,created_at FROM cloud_sphere.notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 100",user);
    }
    public List<Map<String,Object>> payouts(UUID shop) {
        return jdbc.queryForList("SELECT order_id,amount_rwf,created_at FROM cloud_sphere.ledger_movements WHERE credit_account=? ORDER BY created_at DESC", "seller:"+shop);
    }
    public List<Map<String,Object>> ledger() { return jdbc.queryForList("SELECT * FROM cloud_sphere.ledger_movements ORDER BY created_at DESC LIMIT 200"); }
    public Map<String,Object> settings() { return jdbc.queryForMap("SELECT commission_percent,payment_fee_percent FROM cloud_sphere.marketplace_settings WHERE id=1"); }
    public void settings(UUID actor,int commission,int fee) {
        if(commission<0 || fee<0 || commission+fee>50) throw rw.ac.rca.isoko.common.exception.ApiException.badRequest("Fees must total 0–50 percent.");
        String before=settings().toString();
        jdbc.update("UPDATE cloud_sphere.marketplace_settings SET commission_percent=?,payment_fee_percent=? WHERE id=1",commission,fee);
        audit(actor,"MARKETPLACE_FEES_UPDATED",new UUID(0,1),before+" → "+commission+"% commission, "+fee+"% payment fee");
    }
    public List<Map<String,Object>> audit() { return jdbc.queryForList("SELECT * FROM cloud_sphere.audit_events ORDER BY created_at DESC LIMIT 200"); }
}
