# Security Policy

## Supported versions

Security fixes применяются к последней версии default branch.

## Reporting a vulnerability

Не создавайте публичный issue с token, внутренним GitLab URL, stack trace или корпоративным certificate. Используйте private GitHub Security Advisory владельца repository и приложите минимальный synthetic reproduction.

## Credential model

- Сервер использует PAT текущего пользователя со scope `api`.
- PAT хранится только в локальном `.env` и передаётся GitLab через `PRIVATE-TOKEN`.
- `.env`, truststores и логи исключены из Git.
- Token не включается в configuration `toString`, MCP results или request logs.
- HTTP и отключение TLS/hostname verification не поддерживаются.
- Redirect разрешён только внутри configured GitLab origin и URL prefix.

## Before publishing

Перед первым публичным push проверьте всю history, а не только working tree. Например:

```powershell
gitleaks git --redact --no-banner
```

В repository допустимы только synthetic GitLab fixtures. Реальные MR bodies, usernames, internal domains, request IDs, certificates и tokens публиковать нельзя.
