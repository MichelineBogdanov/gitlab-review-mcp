# Contributing

## Requirements

- Java 21;
- Maven 3.6.3+;
- GitLab fixtures должны быть synthetic.

## Quality gate

```powershell
mvn clean verify
```

Изменение считается готовым, когда проходят unit tests, WireMock contracts, ArchUnit, packaged STDIO integration tests, Javadoc doclint и Checkstyle.

## Architecture rules

- `domain` не зависит от application, configuration или infrastructure;
- `application` не зависит от configuration или infrastructure;
- MCP adapters остаются тонкими и не содержат business logic;
- новые GitLab endpoints добавляются через `GitLabClient` port;
- POST не получает automatic retry;
- write-tool не должен принимать comment content повторно после preview.

Паттерны добавляются только при наличии отдельной ответственности или boundary. Не добавляйте generic abstractions для единственной реализации без причины.

## Tests

Для изменения review lifecycle добавляйте state-machine tests. Для GitLab API добавляйте WireMock contract tests с unknown JSON fields и error mapping. Изменения MCP schema должны проверяться packaged STDIO test.

## Public repository hygiene

Не коммитьте `.env`, truststores, internal URLs, реальные GitLab payloads или credentials. Перед push проверяйте staged diff и Git history secret scanner-ом.
