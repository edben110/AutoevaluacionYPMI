# Estado y fuentes del proyecto

Revisión: 6 de octubre de 2026. Esta nota resume lo que se pudo leer del repositorio, su wiki, dos diagramas adjuntos y la Guía 34 enlazada. No reemplaza los artefactos originales.

## Fuentes

- [Repositorio](https://github.com/edben110/AutoevaluacionYPMI) y [wiki](https://github.com/edben110/AutoevaluacionYPMI/wiki). La wiki contiene `Home`, `Glosario` y enlaces al [diagrama de flujo](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-diagrama-de-flujo), [diagrama de clases](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-Diagrama-de-clases) y [diagrama entidad relación](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-Diagrama-DB-Entidad-Relacion).
- [Guía 34 actualizada: Ruta de Mejoramiento Institucional para una Educación de Calidad](https://www.mineducacion.gov.co/portal/Preescolar-basica-y-media/Referentes-de-calidad/430002:Ruta-de-Mejoramiento-Institucional-para-una-Educacion-de-Calidad-Actualizacion-de-la-Guia-N-34), publicada por el Ministerio en 2026. La [wiki enlaza una copia del PDF](https://github.com/user-attachments/files/32343392/articles-430002_recurso_3.pdf), de 189 páginas numeradas más portada.
- La carpeta local `Practicas/evidecia y diagramas` contiene una bitácora de avances de `JuanM`; el equipo confirmó que la evidencia de diseño está en la wiki. El enlace de Lucid al diagrama entidad relación no fue legible desde este entorno; no se atribuyen detalles a ese diagrama.

## Dominio confirmado por la guía

La ruta es un ciclo anual de tres etapas: **autoevaluación institucional** (p. 49), **elaboración anual del PMI** (p. 75) y **seguimiento de avances y resultados** (p. 97). El seguimiento alimenta la autoevaluación siguiente (p. 49). La versión de 2026 mantiene cuatro áreas de gestión y reduce los componentes de 93 a 45 (introducción, p. 10): gestión directiva y estratégica, académica y pedagógica, administrativa y financiera, y de la comunidad (anexo 1, pp. 115-163).

La autoevaluación incluye lectura de contexto, evaluación de áreas/procesos/componentes, perfil institucional y fortalezas y oportunidades de mejora (pp. 51-74). El anexo 2 ofrece una matriz de valoración **1 a 4** por componente, con evidencias (pp. 164-167). El anexo 3 da ejemplos de fuentes de evidencia, expresamente no obligatorios ni exhaustivos (pp. 168-175).

La elaboración del PMI abarca objetivo y factores críticos, metas, indicadores, acciones con responsables, programación anual, recursos y socialización (pp. 77-96). El seguimiento contempla una ruta participativa, comunicación de resultados y retroalimentación del siguiente ciclo (pp. 98-114). Al modelar estas etapas, conservar vínculos entre valoración, evidencia, prioridad, objetivo, meta, acción, indicador y resultado.

## Diseño del equipo en la wiki

El diagrama de flujo distingue ingreso de **SEM** por correo institucional y de **institución** por código DANE. Propone crear autoevaluación con cuatro áreas precargadas, valorar componentes en cuatro niveles (existencia, pertinencia, apropiación, mejoramiento continuo), registrar fortalezas, oportunidades y enlaces/anotaciones de evidencias, y enviar la autoevaluación a revisión de la SEM. Después plantea priorizar componentes, formular objetivo, metas, actividades e indicadores, recopilar resultados/evidencias y permitir observaciones de la SEM sobre autoevaluación y PMI.

El diagrama de clases presenta como diseño `User`, `Establishment`, `Secretary`, `PeriodRecord`, `Area`, `Process`, `Component`, `ComponentValuation`, `PrioritaryComponent`, `PMI`, `Objective`, `Goal`, `Activity` e `Indicator`, entre otros. Es un diseño, no un inventario de clases ya programadas.

## Implementación observada

| Ubicación | Estado al revisar |
| --- | --- |
| `main` | Commit inicial `853d358`; Spring Boot y Vue de arranque. |
| `origin/anton` | Commit `079a03c`; autenticación JWT para SEM e institución, `/api/me`, entidades de usuario, catálogos `Area` → `Process` → `Component`, controladores y servicios iniciales. |
| `JuanM` | Borrador anual e interfaz inicial publicados en `e29f155`, incluyendo los commits de `anton` hasta `079a03c`. La adaptación State y los conteos del 6 de octubre están en cambios locales. |
| `backend` | Java 21, Spring Boot 4.1.1, PostgreSQL, JPA, Flyway, validación y Spring Security. Configuración de BD y JWT mediante variables de entorno o perfil `local` ignorado por Git. V4 agrega el catálogo; V5 agrega el borrador anual y valoraciones por componente. El arranque con PostgreSQL local y `ddl-auto=validate` fue verificado. |
| `frontend` | Vue 3, TypeScript, Vite y router. En `JuanM` local hay acceso de institución/SEM, pantalla de sesión y primera vista de borrador anual. La vista muestra el catálogo cuando tenga componentes activos. La pantalla del PMI sigue pendiente. |

Los controladores de catálogo de `anton` exponen creación y consulta de áreas, procesos y componentes. Su implementación cambia con la rama; verificar código y contratos actuales antes de trabajar sobre ella. La rama `main` no contiene todavía estos avances.

## Primera iteración del borrador anual en `JuanM`

- `PUT /api/self-evaluations/{year}` crea o recupera el borrador de la institución autenticada. `GET /api/self-evaluations/{year}` lo consulta y `GET /api/self-evaluations` lista sus años. Secretaría no puede usar estas rutas.
- `PUT /api/self-evaluations/{year}/valuations/{componentId}` guarda nivel 1-4 y, opcionalmente en el borrador, un enlace HTTP(S) y una nota de evidencia. Solo acepta componentes activos.
- La institución elige manualmente Existencia, Pertinencia, Apropiación o Mejoramiento continuo. El backend aplica State mediante `ValuationState` y cuatro clases concretas; cada estado identifica su código/etiqueta y aporta una unidad a su subtotal. Se mantienen los códigos 1–4 en BD y API para conservar los registros existentes.
- La consulta del borrador añade `totals` por estado y total de valoraciones guardadas. La vista presenta los nombres, subtotales por proceso y totales por área y borrador, sin CSS adicional. Fuente del conteo: el formato Excel descrito por Juan el 6 de octubre (una marca por componente en una sola columna), en correspondencia con la matriz del anexo 2. Detalles en [valoración de componentes](valoracion-componentes.md).
- La implementación supone un borrador por institución y año, protegido por una restricción única en PostgreSQL. La guía describe un ciclo anual; **la unicidad exacta y los estados posteriores** son decisiones de producto que debe confirmar el equipo.
- Fuente funcional: Guía 34 de 2026, etapa de autoevaluación (pp. 49-74) y matriz de valoración con evidencias (anexo 2, pp. 164-167). El diagrama de flujo de la wiki propone el diligenciamiento por componentes y el envío posterior a revisión.
- El catálogo todavía carece de carga inicial. Debe añadirse una migración con las áreas, procesos y componentes oficiales antes de usar la matriz con datos reales.

## Decisiones por confirmar con el equipo

- Qué versión de la Guía 34 y qué instrucciones propias de la ciudad/SEM prevalecen si difieren del PDF de 2026 enlazado en la wiki.
- El diagrama de flujo propone priorizar hasta dos componentes y seleccionar por puntaje de urgencia/tendencia/impacto. Contrastarlo con la metodología de la guía, que parte de oportunidades por las cuatro áreas (p. 77), antes de fijarlo en datos o validaciones.
- Quién puede crear o editar el catálogo de áreas, procesos y componentes, y cómo se publica/versiona para cada año escolar.
- Estados y permisos de borrador, envío, observación, corrección y cierre de autoevaluaciones/PMI; reglas de unicidad por institución y periodo.
- Cómo se seleccionan los componentes candidatos por estados bajos y cómo se priorizan las oportunidades mediante urgencia, tendencia e impacto (Guía 34, pp. 77-80). El subtotal describe la distribución; la priorización requiere mantener las valoraciones individuales. Todavía no existe selección automática de componentes críticos.
- Contrato para evidencias: hoy el flujo dibuja enlaces y notas sobre ubicación; definir almacenamiento, acceso, privacidad y conservación antes de implementar cargas de archivos.

Actualizar esta nota cuando se integren ramas, cambie la wiki o el equipo cierre las decisiones anteriores.
