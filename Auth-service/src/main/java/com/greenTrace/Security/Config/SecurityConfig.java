package com.greenTrace.Security.Config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.*;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableWebSecurity   // Enables Spring Security configuration
public class SecurityConfig {

    // ===========================
    // AUTHORIZATION SERVER CHAIN
    // ===========================
    @Bean
    @Order(1) // Higher priority filter chain
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        // Applies default OAuth2 Authorization Server endpoints
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http
            .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); 
            // Enables OpenID Connect support (/userinfo etc.)

        http
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            )
        	.formLogin(Customizer.withDefaults());

        // If browser hits protected page -> redirect to login

        return http.build();
    }

    // ==========================================
    // CLIENT REGISTRATION (Gateway as OAuth Client)
    // ==========================================
    @Bean
    RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder) {
    	 // Define OAuth2 client (your Gateway)
        RegisteredClient gatewayClient = RegisteredClient.withId("greentrace-gateway-id")
        	// Unique client identifier (must match gateway config)
            .clientId("greentrace-gateway")
            // Client secret (encoded using BCrypt automatically)
            // Gateway will send raw secret → Spring matches using encoder
            .clientSecret(encoder.encode("gateway-secret-2026")) // auto-hash
         // Authentication method used by client 
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/greentrace-gateway")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("emission.read")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(gatewayClient);
    }

    // ==========================
    // NORMAL APP SECURITY CHAIN
    // ==========================
    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(authorize -> authorize

                // Public APIs
                .requestMatchers("/hybridaction/**", "/error").permitAll()

                // Swagger access without login
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/emission/v3/api-docs",
                    "/logistic/v3/api-docs"
                ).permitAll()

                // All remaining endpoints require authentication
                .anyRequest().authenticated()
            )

            .formLogin(Customizer.withDefaults())
            // Enables default Spring login page

            .csrf(csrf -> csrf.ignoringRequestMatchers("/hybridaction/**"));
            // CSRF disabled only for specific APIs

        return http.build();
    }

    // ==========================
    // IN-MEMORY USERS
    // ==========================
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {

        return new InMemoryUserDetailsManager(

            User.withUsername("green_admin")
                .password(encoder.encode("trace2026"))
                .roles("ADMIN")
                .build(),

            User.withUsername("green_user")
                .password(encoder.encode("password123"))
                .roles("USER")
                .build()
        );
    }

        // Good for demo/testing
        // Real project -> DB or Mongo users table

    // ==========================
    // JWT SIGNING KEY SOURCE
    // ==========================
    @Bean
    JWKSource<SecurityContext> jwkSource() {

        KeyPair keyPair = generateRsaKey();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));

        // Public key verifies token
        // Private key signs token
    }

    // Generate RSA key pair dynamically
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // standard secure size
            return keyPairGenerator.generateKeyPair();

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA keys", ex);
        }
    }

    // ==========================
    // CUSTOM JWT CLAIMS
    // ==========================
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {

        return context -> {

            if ("access_token".equals(context.getTokenType().getValue())) {

                context.getClaims().claim(
                    "roles",
                    AuthorityUtils.authorityListToSet(
                        context.getPrincipal().getAuthorities()
                    )
                );
            }
        };

        // Adds user roles into JWT token
        // Example: roles = ROLE_ADMIN
    }
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}