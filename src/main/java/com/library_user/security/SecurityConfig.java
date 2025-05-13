package com.library_user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Configuration
@EnableWebSecurity
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * It defines BCrypt as the encryption algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the security filter chain for traditional Spring MVC (Servlet) requests.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher mvcMatcher = new OrRequestMatcher(
                AntPathRequestMatcher.antMatcher("/api/auth/**"),
                AntPathRequestMatcher.antMatcher("/api/books/**"),
                AntPathRequestMatcher.antMatcher("/api/users/**"),
                AntPathRequestMatcher.antMatcher("/api/borrowings/**"),
                AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                AntPathRequestMatcher.antMatcher("/webjars/**"),
                AntPathRequestMatcher.antMatcher("/swagger-ui.html")
        );

        JwtAuthenticationFilter customJwtAuthFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        http
                .securityMatcher(mvcMatcher)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider(passwordEncoder()))
                .addFilterBefore(customJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines the AuthenticationProvider for MVC authentication.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Provides the standard AuthenticationManager bean from the Spring Security configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    /**
     * Provides a way to load user details reactively.
     */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        return username -> Mono.fromCallable(() -> {
                    try {
                        return userDetailsService.loadUserByUsername(username);
                    } catch (UsernameNotFoundException e) {
                        return null;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userDetails -> userDetails == null ? Mono.error(new UsernameNotFoundException("User not found: " + username)) : Mono.just(userDetails));
    }


    /**
     * Converts the Authorization header in incoming WebFlux requests to an Authentication object containing the Bearer token JWT.

     */
    @Bean
    public ServerAuthenticationConverter jwtAuthenticationConverter() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .map(token -> new UsernamePasswordAuthenticationToken(token, token));
    }


    /**
     * Manages authentication for WebFlux requests.
     */
    @Bean
    public ReactiveAuthenticationManager reactiveJwtAuthenticationManager(ReactiveUserDetailsService reactiveUserDetailsService,
                                                                          JwtUtil jwtUtil) {
        return authentication -> Mono.just(authentication)
                .filter(auth -> auth.getCredentials() instanceof String)
                .map(auth -> (String) auth.getCredentials())
                .flatMap(token -> {
                    String username = jwtUtil.extractUsername(token);
                    return reactiveUserDetailsService.findByUsername(username)
                            .filter(userDetails -> jwtUtil.validateToken(token, userDetails))
                            .map(userDetails -> new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    token,
                                    userDetails.getAuthorities()
                            ))
                            .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid Token or User mismatch")));
                })
                .cast(Authentication.class);
    }


    /**
     * Configures the security filter chain for Reactive (WebFlux) requests.
     */
    @Bean
    @Order(0)
    public SecurityWebFilterChain reactiveSecurityWebFilterChain(ServerHttpSecurity http,
                                                                 ReactiveAuthenticationManager reactiveAuthenticationManager,
                                                                 ServerAuthenticationConverter jwtAuthenticationConverter) {

        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(jwtAuthenticationConverter);

        http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/reactive/**"))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/api/reactive/books/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http.build();
    }
}