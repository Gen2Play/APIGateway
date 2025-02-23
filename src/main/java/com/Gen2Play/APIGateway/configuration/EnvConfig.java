package com.Gen2Play.APIGateway.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    // private final Dotenv dotenv = Dotenv.load();

    // @Bean
    // public Dotenv dotenv() {
    //     return dotenv;
    // }

    // public String getEnv(String key) {
    //     return dotenv.get(key);
    // }

    public static String getEnv(String key) {
        return System.getenv(key);
    }
}