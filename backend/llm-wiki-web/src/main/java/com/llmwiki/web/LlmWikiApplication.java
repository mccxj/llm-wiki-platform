package com.llmwiki.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.llmwiki")
@EnableJpaRepositories(basePackages = "com.llmwiki.domain")
@EntityScan(basePackages = "com.llmwiki.domain")
public class LlmWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmWikiApplication.class, args);
    }
}
