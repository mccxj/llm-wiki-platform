package com.llmwiki.domain;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.llmwiki.domain")
@EntityScan(basePackages = "com.llmwiki.domain")
public class TestApplication {
}
