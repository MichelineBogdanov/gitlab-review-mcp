package ru.bogdanov.gitlabreviewmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the GitLab Review MCP server.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class GitLabReviewMcpApplication {

    /**
     * Starts the STDIO MCP server.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GitLabReviewMcpApplication.class, args);
    }
}
