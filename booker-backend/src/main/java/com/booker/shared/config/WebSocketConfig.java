package com.booker.shared.config;

import com.booker.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Intercept STOMP CONNECT frames to validate JWT token.
     * Sets the authenticated user principal so that convertAndSendToUser routes correctly.
     * Uses userId (from JWT claims) as the principal name for user-destination routing.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.debug("[WS] STOMP CONNECT received — Authorization header present: {}", authHeader != null);
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Object rawUserId = jwtService.extractClaim(token, "userId");
                            log.debug("[WS] Extracted userId claim: {} (type: {})",
                                    rawUserId, rawUserId != null ? rawUserId.getClass().getSimpleName() : "null");
                            String userId = rawUserId.toString();
                            // Principal name = userId so convertAndSendToUser(userId, ...) routes correctly
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
                            accessor.setUser(auth);
                            log.debug("[WS] STOMP CONNECT authenticated — principal set to '{}'", userId);
                        } catch (Exception ex) {
                            log.warn("[WS] STOMP CONNECT rejected — JWT validation failed: {}", ex.getMessage());
                            throw new org.springframework.messaging.MessageDeliveryException(
                                    "Invalid or expired JWT token");
                        }
                    } else {
                        log.warn("[WS] STOMP CONNECT rejected — missing or invalid Authorization header");
                        throw new org.springframework.messaging.MessageDeliveryException(
                                "Missing Authorization header on CONNECT");
                    }
                }
                return message;
            }
        });
    }
}
