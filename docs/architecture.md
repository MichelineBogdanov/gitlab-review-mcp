# Architecture

Проект использует ports-and-adapters. Domain и application не зависят от Spring, MCP или HTTP; направление проверяется ArchUnit.

```mermaid
flowchart LR
    Codex[Codex Desktop / CLI] -->|JSON-RPC over STDIO| Connection[Connection tools - always]
    Codex -->|REPOSITORY| RepositoryAdapter[Repository tools]
    Codex -->|REVIEW| ReviewAdapter[Review tools]

    RepositoryAdapter --> Repository[RepositoryQueryService]
    ReviewAdapter --> Query[MergeRequestQueryService]
    ReviewAdapter --> Prepare[ReviewPreparationService]
    ReviewAdapter --> Publish[ReviewPublicationService]
    Connection --> ConnectionService[GitLabConnectionService]

    Repository --> GitLabPort[GitLabClient port]
    Query --> GitLabPort
    Prepare --> GitLabPort
    Publish --> GitLabPort
    ConnectionService --> GitLabPort
    Prepare --> ProposalPort[ReviewProposalRepository port]
    Publish --> ProposalPort

    GitLabPort --> Rest[GitLab REST adapter]
    Rest --> Retry[GET retry decorator]
    Retry --> Http[JDK HttpClient]
    Http --> GitLab[(Self-hosted GitLab API v4)]

    ProposalPort --> Memory[Thread-safe in-memory repository]
```

Conditional adapters регистрируют взаимоисключающие tool sets. В `REVIEW` исходный код читает Codex из локального workspace, а MCP работает только с MR API. В `REPOSITORY` `RepositoryQueryService` принимает trusted group/project URLs, получает состав групп через Groups API и читает текущее состояние default branch через Project, Repository Tree, Repository Files и Search API. Локальный checkout не используется; repository content не записывается на диск.

## Packages

- `domain` — proposal aggregate, sealed comment hierarchy, atomic publication state;
- `application` — use cases, validation, digest and ports;
- `infrastructure.gitlab` — URL policy, generated MapStruct DTO mapping, bounded HTTP, retry and TLS;
- `infrastructure.proposal` — bounded process-local storage with TTL cleanup;
- `infrastructure.mcp` — annotated tool boundary and structured errors;
- `configuration` — validated Spring configuration and composition root.

## Write boundary

```mermaid
sequenceDiagram
    participant C as Codex
    participant M as MCP server
    participant U as User
    participant G as GitLab

    C->>M: gitlab_prepare_review(MR URL, comments)
    M->>G: GET MR, versions, diffs
    M-->>C: proposalId + digest + full preview
    C->>U: Show complete preview
    U-->>C: Explicit approval
    C->>M: gitlab_publish_review(proposalId, digest)
    M->>G: GET current MR head
    loop each pending comment
        M->>G: POST new discussion
    end
    M-->>C: status + publication receipts
```

`gitlab_publish_review` не принимает comment bodies или positions. Это исключает подмену preview непосредственно перед POST.
