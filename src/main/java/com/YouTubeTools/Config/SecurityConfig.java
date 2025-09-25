package com.YouTubeTools.Config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;

@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/static/**", "/login**").permitAll() // public
                        .anyRequest().authenticated() // everything else requires login
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/transcript", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                );

        return http.build();
    }
}

