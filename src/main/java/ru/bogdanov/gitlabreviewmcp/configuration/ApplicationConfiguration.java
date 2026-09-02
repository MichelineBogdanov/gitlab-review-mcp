package ru.bogdanov.gitlabreviewmcp.configuration;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Clock;
import javax.net.ssl.SSLContext;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bogdanov.gitlabreviewmcp.application.DiffPositionValidator;
import ru.bogdanov.gitlabreviewmcp.application.GitLabConnectionService;
import ru.bogdanov.gitlabreviewmcp.application.MergeRequestQueryService;
import ru.bogdanov.gitlabreviewmcp.application.ProposalDigestService;
import ru.bogdanov.gitlabreviewmcp.application.ReviewPreparationService;
import ru.bogdanov.gitlabreviewmcp.application.ReviewPublicationService;
import ru.bogdanov.gitlabreviewmcp.application.model.ReviewLimits;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.application.port.MergeRequestReferenceParser;
import ru.bogdanov.gitlabreviewmcp.application.port.ReviewProposalRepository;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.GitLabDtoMapper;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.GitLabHttpTransport;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.GitLabMergeRequestUrlParser;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.JdkGitLabClient;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.JdkGitLabHttpTransport;
import ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab.RetryingGitLabHttpTransport;
import ru.bogdanov.gitlabreviewmcp.infrastructure.proposal.InMemoryReviewProposalRepository;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Explicit composition root for application ports and adapters.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    /** @return UTC application clock */
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    /** @return canonical JSON mapper with tolerant input mapping */
    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** @param properties GitLab settings @return configured corporate SSL context */
    @Bean
    SSLContext gitLabSslContext(GitLabProperties properties) {
        return new SslContextFactory().create(properties.getSsl());
    }

    /** @param properties GitLab settings @param sslContext trusted SSL context @return redirect-disabled client */
    @Bean
    HttpClient gitLabHttpClient(GitLabProperties properties, SSLContext sslContext) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** @param properties GitLab settings @return strict merge request parser */
    @Bean
    MergeRequestReferenceParser mergeRequestReferenceParser(GitLabProperties properties) {
        return new GitLabMergeRequestUrlParser(properties.getBaseUrl());
    }

    /** @return GitLab DTO mapper */
    @Bean
    GitLabDtoMapper gitLabDtoMapper() {
        return Mappers.getMapper(GitLabDtoMapper.class);
    }

    /**
     * Creates the authenticated retry-decorated transport.
     *
     * @param client JDK HTTP client
     * @param properties GitLab settings
     * @return GitLab transport
     */
    @Bean
    GitLabHttpTransport gitLabHttpTransport(HttpClient client, GitLabProperties properties) {
        GitLabHttpTransport raw = new JdkGitLabHttpTransport(
                client, properties.getBaseUrl(), properties.getToken());
        return new RetryingGitLabHttpTransport(raw, properties.getMaxRetries());
    }

    /** @return GitLab REST adapter */
    @Bean
    GitLabClient gitLabClient(
            GitLabProperties properties,
            GitLabHttpTransport transport,
            ObjectMapper objectMapper,
            GitLabDtoMapper mapper) {
        return new JdkGitLabClient(properties, transport, objectMapper, mapper);
    }

    /** @return bounded in-memory proposal repository */
    @Bean
    ReviewProposalRepository reviewProposalRepository(ReviewProperties properties, Clock clock) {
        return new InMemoryReviewProposalRepository(properties.proposalMaxCount(), clock);
    }

    /** @return connection diagnostic service */
    @Bean
    GitLabConnectionService gitLabConnectionService(GitLabClient client) {
        return new GitLabConnectionService(client);
    }

    /** @return merge request read service */
    @Bean
    MergeRequestQueryService mergeRequestQueryService(
            MergeRequestReferenceParser parser,
            GitLabClient client) {
        return new MergeRequestQueryService(parser, client);
    }

    /** @return review preparation service */
    @Bean
    ReviewPreparationService reviewPreparationService(
            MergeRequestReferenceParser parser,
            GitLabClient client,
            ReviewProposalRepository repository,
            ObjectMapper objectMapper,
            ReviewProperties properties,
            Clock clock) {
        return new ReviewPreparationService(
                parser,
                client,
                repository,
                new ProposalDigestService(objectMapper),
                new DiffPositionValidator(),
                new ReviewLimits(
                        properties.proposalTtl(), properties.maxComments(), properties.maxCommentLength()),
                clock,
                new SecureRandom());
    }

    /** @return review publication service */
    @Bean
    ReviewPublicationService reviewPublicationService(
            GitLabClient client,
            ReviewProposalRepository repository,
            Clock clock) {
        return new ReviewPublicationService(client, repository, clock);
    }
}
