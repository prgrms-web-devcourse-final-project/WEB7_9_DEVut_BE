package devut.buzzerbidder.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final SessionCookieRemovalFilter sessionCookieRemovalFilter;
    private final ApplicationContext applicationContext;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 모든 주소에서 접근 가능하도록 설정
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Preflight 요청 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                .anyRequest().permitAll() // 임시로 모든 요청 허용
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(sessionCookieRemovalFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // OAuth2 ClientRegistrationRepository가 있을 때만 OAuth2 로그인 활성화
        try {
            ClientRegistrationRepository clientRegistrationRepository =
                applicationContext.getBean(ClientRegistrationRepository.class);
            if (clientRegistrationRepository != null) {
                // OAuth2 로그인 시 세션을 사용하지 않도록 설정
                SecurityContextRepository securityContextRepository = new NullSecurityContextRepository();
                http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                    )
                    .successHandler(oAuth2SuccessHandler)
                    // OAuth2 로그인 플로우에서도 세션을 사용하지 않도록 명시
                    .securityContextRepository(securityContextRepository)
                );
            }
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            // OAuth2 설정이 없으면 OAuth2 로그인을 비활성화 (테스트 환경 등)
        }
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

}