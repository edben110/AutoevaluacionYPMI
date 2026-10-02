# Instrucciones del proyecto AutoevaluacionYPMI

- Lee `docs/estado-proyecto.md` antes de diseñar una funcionalidad del dominio. Contrasta siempre la wiki, la Guía 34 enlazada allí y el código de la rama en la que trabajas. Distingue diseño previsto de comportamiento implementado.
- Comprueba `git status` y las ramas antes de editar. `main` contiene la base inicial; `JuanM` incorporó localmente la autenticación y catálogos de `anton` al 1 de octubre de 2026. No sobrescribas trabajo de otros desarrolladores ni cambies de rama sin tener en cuenta cambios locales.
- Mantén trazabilidad de cada regla funcional con la sección o página de la Guía 34 de 2026 y, cuando aplique, el diagrama o decisión del equipo. Si una regla municipal no está documentada, registra la duda y evita presentarla como requisito confirmado.
- Conserva la separación entre institución educativa y Secretaría de Educación Municipal (SEM). Verifica autorización por rol y pertenencia de los datos a la institución, además de la autenticación.
- En backend, al añadir o cambiar entidades persistentes, revisa su migración Flyway y la compatibilidad con `spring.jpa.hibernate.ddl-auto=validate`. No incluyas credenciales, tokens ni secretos en el repositorio.
- En frontend, usa Vue 3 con TypeScript y las rutas/API reales del proyecto; no diseñes pantallas suponiendo endpoints que todavía no existen. Trata valoraciones, evidencias y seguimiento como datos verificables y comprensibles para usuarios educativos.
- Para cambios funcionales, verifica el comportamiento relevante con las herramientas existentes (`backend/gradlew.bat test`, `npm run build` en `frontend`, según el área modificada). Documenta bloqueos reales, como servicios externos o base de datos faltantes.

Las skills del proyecto están en `.agents/skills/` y los agentes especializados en `.codex/agents/`.
