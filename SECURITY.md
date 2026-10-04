# Security Policy · Política de seguridad

## Supported versions · Versiones soportadas

Only the latest release (and `main`) receives security fixes.
Solo la última versión publicada (y `main`) recibe correcciones de seguridad.

| Version · Versión | Supported · Soportada |
|---|---|
| latest `1.x` · última `1.x` | ✅ |
| older · anteriores | ❌ upgrade · actualiza |

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

## Supply chain · Cadena de suministro

- Every release is built by GitHub Actions from a tag, ships a CycloneDX **SBOM**, a build-provenance **attestation**
  and keyless **Sigstore** signatures; verify them as described in
  [docs/en/advanced-testing.md](docs/en/advanced-testing.md#supply-chain-quality-gates).
  Cada release se construye en GitHub Actions desde un tag, incluye un **SBOM** CycloneDX, una **atestación** de
  procedencia y firmas **Sigstore** sin llaves; verifícalas como indica
  [docs/es/pruebas-avanzadas.md](docs/es/pruebas-avanzadas.md#controles-de-calidad-y-cadena-de-suministro).
- GitHub Actions are pinned to commit SHAs; dependencies are scanned with OSV every night and with dependency review on
  pull requests; Dependabot proposes updates weekly.
  Las GitHub Actions están fijadas a SHA de commit; las dependencias se analizan con OSV cada noche y con dependency
  review en los pull requests; Dependabot propone actualizaciones cada semana.
- Security advisories are published in the repository's **Security → Advisories** tab.
  Los avisos de seguridad se publican en la pestaña **Security → Advisories** del repositorio.
