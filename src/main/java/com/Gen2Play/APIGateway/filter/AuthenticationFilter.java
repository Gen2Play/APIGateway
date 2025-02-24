package com.Gen2Play.APIGateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final SecretKey key;

    public AuthenticationFilter(@Value("${jwt.secret}") String secretKey) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public static class Config {
        // Có thể cấu hình thêm nếu cần
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            System.out.println("🔍 [AuthenticationFilter] Incoming request: " + path);

            // ✅ Bỏ qua filter nếu request thuộc API authentication
            if (path.startsWith("/api/auth/") || path.startsWith("/swagger")
                    || path.startsWith("/swagger-ui")
                    || path.startsWith("/v3/api-docs")
                    || path.startsWith("/swagger-resources")
                    || path.startsWith("/webjars")) {
                System.out.println("✅ [AuthenticationFilter] Skipping authentication for: " + path);
                return chain.filter(exchange);
            }
            
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // ❌ Nếu không có header Authorization hoặc sai định dạng
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("❌ [AuthenticationFilter] Missing or invalid Authorization header.");
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // ✅ Lấy accountId từ JWT
                String accountId = claims.getSubject();
                System.out.println("✅ [AuthenticationFilter] accountId: " + accountId);

                // ✅ Lấy danh sách permissions từ JWT
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);
                String permissionsStr = permissions != null ? String.join(",", permissions) : "";
                System.out.println("✅ [AuthenticationFilter] permissions: " + permissionsStr);

                // ✅ Thêm thông tin vào Header
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-Account-Id", accountId)
                        .header("X-Permissions", permissionsStr)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (SignatureException e) {
                System.out.println("❌ [AuthenticationFilter] Invalid JWT Token: " + e.getMessage());
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }
        };
    }
}
