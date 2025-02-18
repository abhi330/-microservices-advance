package com.microservice.apigateway.filter;

import com.microservice.apigateway.exception.JwtTokenMalformedException;
import com.microservice.apigateway.exception.JwtTokenMissingException;
import com.microservice.apigateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        log.info("JwtAuthenticationFilter | filter is working");

        ServerHttpRequest request = (ServerHttpRequest) exchange.getRequest();

        final List<String> apiEndpoints = List.of("/signup", "/login","/refreshtoken");

        Predicate<ServerHttpRequest> isApiSecured = r -> apiEndpoints.stream()
                .noneMatch(uri -> r.getURI().getPath().contains(uri));

        log.info("JwtAuthenticationFilter | filter | isApiSecured.test(request) : " + isApiSecured.test(request));

        if (isApiSecured.test(request)) {
            if (!request.getHeaders().containsKey("Authorization")) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);

                return response.setComplete();
            }

            final String authorization = request.getHeaders().getOrEmpty("Authorization").get(0);
            final String token = authorization.replace("Bearer ", "");

            log.info("JwtAuthenticationFilter | filter | token : " + token);

            try {
                jwtUtils.validateJwtToken(token);
            } catch (ExpiredJwtException e) {
                log.info("JwtAuthenticationFilter | filter | ExpiredJwtException | error : " + e.getMessage());
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);

                return response.setComplete();

            } catch (IllegalArgumentException | JwtTokenMalformedException | JwtTokenMissingException
                     | SignatureException | UnsupportedJwtException e) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.BAD_REQUEST);

                return response.setComplete();
            }

//            Claims claims = jwtUtils.getClaims(token);
//            exchange.getRequest().mutate()
//                    .header("username", String.valueOf(claims.getSubject()))
//                    .build();
            Claims claims = jwtUtils.getClaims(token);
            String username = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            Set<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);

            SecurityContext securityContext = new SecurityContextImpl(authentication);

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));



        }

        return chain.filter(exchange);
    }
}
