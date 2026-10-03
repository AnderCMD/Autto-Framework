# Secretos y variables de entorno

[← Volver al README](../../README.es.md) · [English](../en/secrets.md)

> ¿Por qué `.env` y no otra cosa? Ver [ADR-003 en Decisiones de arquitectura](decisiones.md).

## Desarrollo local

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

Edita `.env` (está ignorado por git):

```dotenv
SAUCE_PASSWORD=secret_sauce          # valor de demo publicado por saucedemo.com
AUTTO_BROWSER_NAME=firefox           # cualquier ajuste autto.* se puede sobrescribir
SPRING_PROFILES_ACTIVE=qa
```

El archivo se busca en el directorio de trabajo o hasta tres carpetas padre, así funciona desde la raíz del
repositorio, desde un módulo y desde el IDE. Usa `-Dautto.dotenv.path=/ruta/.env` (o `AUTTO_DOTENV_PATH`) para otra
ubicación.

Sintaxis soportada: `CLAVE=valor`, `export CLAVE=valor`, `# comentarios`, `'comillas simples'` (literal),
`"comillas dobles"` (escapes, multilínea).

## Usar un secreto

Referéncialo desde `application.yml`; nunca escribas el valor:

```yaml
test-data:
  users:
    admin:
      username: admin@empresa.com
      password: ${ADMIN_PASSWORD}
```

```java
Credentials admin = users.get("admin");     // falla con un mensaje claro si falta ADMIN_PASSWORD
loginPage.loginAs(admin);
```

Otras opciones:

- `AuttoSettings.get().find("ADMIN_PASSWORD")` en cualquier parte del código.
- `@Value("${ADMIN_PASSWORD}")` o `Environment` en beans de Spring.
- Placeholders `${ADMIN_PASSWORD}` dentro de datos de prueba JSON (`TestData`).

## Precedencia

```
propiedad -D  >  variable de entorno real  >  .env  >  application-<perfil>.yml  >  application.yml
```

Así un secreto del CI nunca queda oculto por un `.env` desactualizado.

## CI

Declara los mismos nombres como secretos del pipeline:

| Plataforma | Dónde |
|---|---|
| GitHub Actions | *Settings → Secrets and variables → Actions*, luego `env: SAUCE_PASSWORD: ${{ secrets.SAUCE_PASSWORD }}` |
| GitLab CI | *Settings → CI/CD → Variables* (masked + protected) |
| Jenkins | Credentials + `withCredentials([string(credentialsId: 'sauce-password', variable: 'SAUCE_PASSWORD')])` |
| Azure DevOps | Variable groups / variables enlazadas a Key Vault |

## Enmascarado

Cada variable cuyo **nombre** parece un secreto (`*PASSWORD*`, `*SECRET*`, `*TOKEN`, `*API_KEY`, `*ACCESS_KEY`,
`*CREDENTIAL*`…) se registra al iniciar la ejecución. Su valor se reemplaza por `******` en:

- el reporte Extent (nombres de steps, logs, tablas, JSON, código fuente de páginas),
- el log de ejecución (`%maskedMsg` en `logback-test.xml`),
- `Credentials.toString()`.

Registra otros valores manualmente con `Secrets.register(valor)`.

> Los reportes nativos de Cucumber (`cucumber.html`, `cucumber.json`) los escribe Cucumber y no se enmascaran:
> nunca pases secretos como argumentos literales de un step ni los registres con `scenario.log(...)`.

## Prevención de fugas

- `.gitignore`: `.env`, `.env.*` (salvo `.env.example`), `application-local.yml`.
- El job de CI **Secret scanning (gitleaks)** revisa todo el historial de git en cada push y pull request.
- Hook local: `pip install pre-commit && pre-commit install` ejecuta gitleaks antes de cada commit.
- Si aun así se filtra un secreto: **rótalo primero**, después limpia el historial.

## Ir más allá: gestores de secretos

En empresas, mantén `.env` para trabajo local y carga el resto desde un vault. Como el framework lee propiedades
estándar de Spring, los mismos nombres funcionan con los starters de Spring Cloud Vault, Azure Key Vault, AWS Secrets
Manager o GCP Secret Manager, o con inyección por CLI (`op run --env-file=.env -- ./mvnw test`,
`doppler run -- ./mvnw test`).
