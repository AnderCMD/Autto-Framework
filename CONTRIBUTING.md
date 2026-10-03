# Contributing · Contribuir

Thanks for helping to improve Autto! · ¡Gracias por ayudar a mejorar Autto!

## English

1. Fork the repository and create a branch: `feat/<short-name>` or `fix/<short-name>`.
2. Make your change following the conventions below.
3. Run the checks:
   ```bash
   ./mvnw verify -Dcucumber.filter.tags=@showcase      # enforcer, checkstyle, unit tests, report pipeline
   ./mvnw install -Dautto.browser.headless=true        # full suite (if you touched the driver or hooks)
   ```
4. Open a pull request using the template. Use [Conventional Commits](https://www.conventionalcommits.org/)
   (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`, `chore:`).

### Conventions

- **All code, identifiers, comments, logs and commit messages are in English.**
- **Documentation is bilingual**: every change to `docs/en` must be mirrored in `docs/es` (and `README.md` ↔
  `README.es.md`).
- Engine code goes to `autto-core` and must not know about any business feature.
- Test code goes to `autto-e2e`, organized by business feature (`features/<feature>`), never by technical layer.
- Page objects are annotated with `@PageObject`; dependencies are injected through constructors.
- Never commit secrets: values go to `.env` (git-ignored) or CI secrets; only names go to `.env.example`.
  Enable the hooks with `pre-commit install`.
- Page objects: `private static final By` locators, no assertions, no stored `WebElement`/`WebDriver`.
- No `Thread.sleep` in tests; use explicit waits.
- New configuration keys: add them to `AuttoProperties` (with `@param` Javadoc), `application.yml` and both
  configuration guides.
- Architecture-relevant changes need a new entry in `docs/en/decisions.md` and `docs/es/decisiones.md`.
- Follow `.editorconfig` (4 spaces, 120 columns, LF).

## Español

1. Haz un fork del repositorio y crea una rama: `feat/<nombre-corto>` o `fix/<nombre-corto>`.
2. Realiza tu cambio siguiendo las convenciones.
3. Ejecuta las comprobaciones:
   ```bash
   ./mvnw verify -Dcucumber.filter.tags=@showcase      # enforcer, checkstyle, tests unitarios, reportes
   ./mvnw install -Dautto.browser.headless=true        # suite completa (si tocaste driver o hooks)
   ```
4. Abre un pull request usando la plantilla. Usa [Conventional Commits](https://www.conventionalcommits.org/es/)
   (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`, `chore:`).

### Convenciones

- **Todo el código, identificadores, comentarios, logs y mensajes de commit van en inglés.**
- **La documentación es bilingüe**: cada cambio en `docs/en` debe reflejarse en `docs/es` (y `README.md` ↔
  `README.es.md`).
- El código del motor va en `autto-core` y no debe conocer ninguna funcionalidad de negocio.
- El código de pruebas va en `autto-e2e`, organizado por funcionalidad de negocio (`features/<feature>`), nunca por
  capa técnica.
- Los page objects llevan `@PageObject`; las dependencias se inyectan por constructor.
- Nunca subas secretos: los valores van en `.env` (ignorado por git) o en los secretos del CI; en `.env.example`
  solo van los nombres. Activa los hooks con `pre-commit install`.
- Page objects: locators `private static final By`, sin aserciones, sin guardar `WebElement`/`WebDriver`.
- Nada de `Thread.sleep` en las pruebas; usa esperas explícitas.
- Nuevas claves de configuración: añádelas a `AuttoProperties` (con Javadoc `@param`), a `application.yml` y a
  ambas guías de configuración.
- Los cambios relevantes de arquitectura requieren una entrada en `docs/en/decisions.md` y `docs/es/decisiones.md`.
- Sigue `.editorconfig` (4 espacios, 120 columnas, LF).
