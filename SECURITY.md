# Security Policy · Política de seguridad

## Supported versions · Versiones soportadas

Only the latest release on the `main` branch receives security fixes.
Solo la última versión de la rama `main` recibe correcciones de seguridad.

## Reporting a vulnerability · Reportar una vulnerabilidad

**Do not open a public issue.** Use GitHub's
[private vulnerability reporting](https://github.com/AnderCMD/Autto-Framework/security/advisories/new).
You will get an answer within 7 days.

**No abras un issue público.** Usa el
[reporte privado de vulnerabilidades](https://github.com/AnderCMD/Autto-Framework/security/advisories/new) de GitHub.
Recibirás respuesta en un máximo de 7 días.

## Secrets in test automation · Secretos en la automatización

- Never commit credentials: reference them with `${NAME}` in `application.yml` / test data, keep values in the
  git-ignored `.env` locally and in CI secrets in pipelines. See [docs/en/secrets.md](docs/en/secrets.md).
- Nunca subas credenciales: referéncialas con `${NOMBRE}` en `application.yml` / datos de prueba, guarda los valores en
  el `.env` (ignorado por git) en local y en los secretos del CI. Ver [docs/es/secretos.md](docs/es/secretos.md).
- Secret values are masked in logs and reports; remote URLs with credentials are redacted.
- gitleaks scans every push / pull request and can run as a pre-commit hook (`pre-commit install`).
