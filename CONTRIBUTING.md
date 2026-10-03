# Contributing · Contribuir

Thanks for helping to improve Autto! · ¡Gracias por ayudar a mejorar Autto!

## English

1. Fork the repository and create a branch: `feat/<short-name>` or `fix/<short-name>`.
2. Make your change following the conventions below.
3. Run the checks:
   ```bash
   ./mvnw -Punit test                                  # framework unit tests
   ./mvnw test -Dcucumber.filter.tags=@showcase        # report pipeline
   ./mvnw test -Dbrowser.headless=true                 # full suite (if you touched the driver or hooks)
   ```
4. Open a pull request using the template. Use [Conventional Commits](https://www.conventionalcommits.org/)
   (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`, `chore:`).

### Conventions

- **All code, identifiers, comments, logs and commit messages are in English.**
- **Documentation is bilingual**: every change to `docs/en` must be mirrored in `docs/es` (and `README.md` ↔
  `README.es.md`).
- Framework code goes to `src/main/java/.../core` and must not know about any business feature.
- Test code is organized by business feature (`features/<feature>`), never by technical layer.
- Page objects: `private static final By` locators, no assertions, no stored `WebElement`/`WebDriver`.
- No `Thread.sleep` in tests; use explicit waits.
- New configuration keys: add them to `ConfigKeys`, `autto.properties` (commented) and both configuration guides.
- Follow `.editorconfig` (4 spaces, 120 columns, LF).

## Español

1. Haz un fork del repositorio y crea una rama: `feat/<nombre-corto>` o `fix/<nombre-corto>`.
2. Realiza tu cambio siguiendo las convenciones.
3. Ejecuta las comprobaciones:
   ```bash
   ./mvnw -Punit test                                  # tests unitarios del framework
   ./mvnw test -Dcucumber.filter.tags=@showcase        # pipeline de reportes
   ./mvnw test -Dbrowser.headless=true                 # suite completa (si tocaste driver o hooks)
   ```
4. Abre un pull request usando la plantilla. Usa [Conventional Commits](https://www.conventionalcommits.org/es/)
   (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`, `chore:`).

### Convenciones

- **Todo el código, identificadores, comentarios, logs y mensajes de commit van en inglés.**
- **La documentación es bilingüe**: cada cambio en `docs/en` debe reflejarse en `docs/es` (y `README.md` ↔
  `README.es.md`).
- El código del framework va en `src/main/java/.../core` y no debe conocer ninguna funcionalidad de negocio.
- El código de pruebas se organiza por funcionalidad de negocio (`features/<feature>`), nunca por capa técnica.
- Page objects: locators `private static final By`, sin aserciones, sin guardar `WebElement`/`WebDriver`.
- Nada de `Thread.sleep` en las pruebas; usa esperas explícitas.
- Nuevas claves de configuración: añádelas a `ConfigKeys`, a `autto.properties` (comentadas) y a ambas guías de
  configuración.
- Sigue `.editorconfig` (4 espacios, 120 columnas, LF).
