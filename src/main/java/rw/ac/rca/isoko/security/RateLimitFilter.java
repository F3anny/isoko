package rw.ac.rca.isoko.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    public RateLimitFilter(StringRedisTemplate redis) {this.redis=redis;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getRequestURI();
        boolean limited=req.getMethod().equals("POST") && (path.startsWith("/api/v1/auth/") || path.equals("/api/v1/checkout") || path.endsWith("/pay") || path.endsWith("/confirm-delivery") || path.equals("/api/v1/payments/callback"));
        if(limited) {
            String key="isoko:rate:"+path+":"+req.getRemoteAddr();
            var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if(exceeded(key,60) || (authentication!=null && authentication.getPrincipal() instanceof AuthPrincipal p && exceeded("isoko:rate-user:"+path+":"+p.userId(),20))) {
                res.setStatus(429);res.setHeader("Retry-After","60");res.setContentType("application/problem+json");
                String trace=String.valueOf(req.getAttribute("traceId"));
                res.getWriter().write("{\"status\":429,\"code\":\"AUTH-429\",\"detail\":\"Too many attempts. Try again in one minute.\",\"traceId\":\""+trace+"\"}");return;
            }
        }
        chain.doFilter(req,res);
    }
    private boolean exceeded(String key,int limit) {
        Long count=redis.opsForValue().increment(key);if(Long.valueOf(1).equals(count))redis.expire(key,Duration.ofMinutes(1));return count!=null&&count>limit;
    }
}
