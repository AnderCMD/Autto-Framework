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

- Never commit credentials. Use `${placeholders}` in test data and provide values through `AUTTO_*` environment
  variables or CI secrets.
- Nunca subas credenciales. Usa `${placeholders}` en los datos de prueba y define los valores con variables de entorno
  `AUTTO_*` o secretos del CI.
- Remote URLs with credentials (`https://user:key@hub`) are redacted in logs and reports.
