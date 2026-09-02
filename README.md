# GitLab Review MCP

Локальный STDIO MCP-сервер для чтения и ревью merge requests в self-hosted GitLab. Сервер работает как узкий прокси между Codex и GitLab REST API v4: он не клонирует repository, не меняет локальные файлы и не предоставляет операции merge, push, approve или resolve.

Целевой runtime:

- Java 21;
- Maven 3.6.3+;
- Spring Boot 4.1.1;
- Spring AI 2.0.1;
- MapStruct 1.6.3;
- GitLab 17.2.1 и новее.

## Граница публикации

Чтение MR выполняется автоматически. Публикация ревью разделена на две операции:

1. `gitlab_prepare_review` проверяет комментарии по текущему diff, создаёт полный preview, digest и временный proposal в памяти процесса. POST-запросов к GitLab на этом этапе нет.
2. Codex показывает пользователю весь preview.
3. Только после явного подтверждения `gitlab_publish_review` получает `proposalId` и `expectedDigest` и публикует сохранённые комментарии. Текст и позиции повторно передать в write-tool нельзя.

Proposal живёт 15 минут по умолчанию и теряется при завершении MCP-процесса. Изменение head SHA делает proposal устаревшим. Каждый комментарий публикуется отдельной GitLab discussion.

## MCP tools

| Tool | Операция |
|---|---|
| `gitlab_check_connection` | Проверяет `/version`, `/user`, minimum GitLab version и PAT |
| `gitlab_get_merge_request` | Возвращает metadata и current head SHA |
| `gitlab_get_merge_request_diff` | Возвращает bounded page изменённых файлов и unified diff |
| `gitlab_get_merge_request_discussions` | Возвращает discussions, replies, positions и resolved state |
| `gitlab_prepare_review` | Проверяет comments и сохраняет immutable preview в памяти |
| `gitlab_publish_review` | Публикует ранее подготовленный proposal |

MR принимается только полным URL на настроенном GitLab origin:

```text
https://gitlab.example.com/group/project/-/merge_requests/123
```

Scheme, host, effective port и GitLab URL prefix должны совпадать с `gitlab.base-url`. Project path кодируется сервером. Redirect на другой origin или за пределы настроенного prefix отклоняется до отправки PAT.

## Сборка

```powershell
java -version
mvn -version
mvn clean verify
```

Результат — executable fat JAR:

```text
target/gitlab-review-mcp.jar
```

`verify` запускает unit-тесты, WireMock contract tests, ArchUnit, packaged STDIO integration tests, JaCoCo, Javadoc doclint и Checkstyle. Maven Wrapper намеренно не добавлен.

## Personal Access Token

Создайте отдельный PAT в настройках своей учётной записи GitLab:

- scope: `api`;
- короткий срок действия;
- понятное имя, например `gitlab-review-mcp-local`.

Scope `api` нужен как для чтения MR, так и для создания discussions. Не добавляйте token в Codex config, JVM arguments или Git history.

## `.env`

Скопируйте пример и заполните локальный файл:

```powershell
Copy-Item .env.example .env
```

Минимальная конфигурация:

```properties
gitlab.base-url=https://gitlab.example.com
gitlab.token=replace-me
```

Полный пример находится в [`.env.example`](.env.example). `.env` игнорируется Git. Spring Boot загружает extensionless properties-файл через `spring.config.import`; дополнительная dotenv-библиотека не используется.

Если GitLab установлен с relative URL root, включите prefix в base URL:

```properties
gitlab.base-url=https://intranet.example.com/gitlab
```

Обычный HTTP, отключение hostname verification и trust-all TLS не поддерживаются.

## Corporate CA

Создайте отдельный PKCS12 truststore с корпоративным CA, не меняя глобальный JDK truststore:

```powershell
keytool -importcert `
  -alias corporate-root-ca `
  -file C:\certificates\corporate-root-ca.cer `
  -keystore C:\secure\gitlab-review-truststore.p12 `
  -storetype PKCS12
```

Добавьте путь и пароль в `.env`:

```properties
gitlab.ssl.trust-store-path=C:/secure/gitlab-review-truststore.p12
gitlab.ssl.trust-store-password=replace-me
gitlab.ssl.trust-store-type=PKCS12
```

Truststores (`*.jks`, `*.p12`, `*.pfx`) исключены из Git.

## Подключение к Codex Desktop и CLI

Сначала соберите JAR, затем добавьте сервер в Codex `config.toml`:

```toml
[mcp_servers.gitlab_review]
command = "java"
args = [
  "-jar",
  "C:\\absolute\\path\\gitlab-review-mcp\\target\\gitlab-review-mcp.jar"
]
cwd = "C:\\absolute\\path\\gitlab-review-mcp"
enabled = true
startup_timeout_sec = 20
tool_timeout_sec = 120
default_tools_approval_mode = "auto"
enabled_tools = [
  "gitlab_check_connection",
  "gitlab_get_merge_request",
  "gitlab_get_merge_request_diff",
  "gitlab_get_merge_request_discussions",
  "gitlab_prepare_review",
  "gitlab_publish_review"
]

[mcp_servers.gitlab_review.tools.gitlab_publish_review]
approval_mode = "prompt"
```

`cwd` должен указывать на корень проекта, потому что `.env` загружается относительно working directory. Локальные STDIO servers и per-tool approval настраиваются средствами [Codex MCP configuration](https://developers.openai.com/codex/mcp/).

После изменения конфигурации перезапустите Codex Desktop. Проверка:

```powershell
codex mcp list
```

В Desktop список доступен через `/mcp`. После успешного подключения вызовите `gitlab_check_connection`.

## Сценарий: доработка собственного MR

В существующей задаче Codex передайте URL своего MR и попросите учесть замечания. Агент:

1. читает metadata, diff и все страницы discussions;
2. сопоставляет unresolved comments и replies с локальным workspace и контекстом задачи;
3. продолжает реализацию и запускает тесты;
4. не использует GitLab MCP для изменения repository content.

## Сценарий: ревью чужого MR

В отдельной задаче Codex передайте URL MR и дополнительный контекст: Jira, Confluence или требования. Агент:

1. читает metadata, diff и existing discussions;
2. проверяет реализацию относительно переданного контекста;
3. формирует потенциальные замечания;
4. вызывает `gitlab_prepare_review`;
5. показывает полный preview с `proposalId`, digest и expiration;
6. ждёт явного подтверждения;
7. вызывает `gitlab_publish_review` через approval prompt.

Jira и Confluence остаются контекстом Codex и не передаются этому MCP-серверу.

## Partial и unknown publication

Состояния proposal:

- `PREPARED` — preview готов;
- `PUBLISHING` — выполняется последовательная публикация;
- `PARTIAL` — часть comments опубликована, гарантированно неотправленные можно повторить;
- `PUBLISHED` — все discussion созданы;
- `UNKNOWN` — outcome POST неизвестен, автоматический retry запрещён;
- `EXPIRED` — TTL истёк.

При deterministic GitLab error уже созданные discussion IDs сохраняются, а повторный approved publish отправляет только `PENDING` comments. Timeout или I/O failure после начала POST переводит item в `UNKNOWN`: перечитайте discussions и подготовьте новое ревью, чтобы не создать дубликат.

## Ограничения

По умолчанию:

- 50 comments на proposal;
- 10 000 символов на comment;
- 500 changed files;
- 10 MB на JSON response;
- 5 MB diff content на page;
- 100 proposals в памяти;
- TTL proposal 15 минут.

Для больших MR tools возвращают `nextCursor`, `truncated` и `warnings`. Перед анализом нужно пройти pagination до отсутствия `nextCursor`.

## Диагностика

| Симптом | Проверка |
|---|---|
| MCP startup failure | Java 21, абсолютный путь к JAR, `cwd`, наличие `.env` |
| `GITLAB_UNAUTHORIZED` / HTTP 401 | PAT существует, не истёк и корректно скопирован |
| `GITLAB_FORBIDDEN` / HTTP 403 | scope `api` и доступ пользователя к project/MR |
| TLS handshake error | corporate CA импортирован в указанный truststore |
| timeout | GitLab/VPN доступен, увеличьте `gitlab.read-timeout` |
| `UNSUPPORTED_GITLAB_VERSION` | требуется GitLab 17.2.1+ |
| `INVALID_DIFF_POSITION` | head/diff изменился или line отсутствует на выбранной стороне |
| `PROPOSAL_NOT_FOUND` | MCP был перезапущен или истёк TTL; выполните prepare заново |
| stale head | перечитайте MR и сформируйте новый proposal |

Логи идут только в `stderr`; `stdout` зарезервирован для MCP JSON-RPC. Token, request headers, `.env`, environment и stack traces не входят в tool responses.

## Архитектура

Подробная схема и правила зависимостей: [`docs/architecture.md`](docs/architecture.md).

## Разработка и безопасность

- [`CONTRIBUTING.md`](CONTRIBUTING.md)
- [`SECURITY.md`](SECURITY.md)

Лицензия намеренно не добавлена.
