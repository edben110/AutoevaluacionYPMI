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
| `origin/anton` | Commit `f0b18b0`, consultado el 6 de octubre de 2026. Además de autenticación y catálogos, incorpora actualización, activación/desactivación, cambio de área/proceso padre y captura de `expectedEvidence` al crear/editar componentes. Sus cinco commits posteriores a `c8e2bec` incluyen una integración previa de `JuanM` y comentarios. |
| `JuanM` | Borrador e interfaz inicial en `e29f155`; limpieza de agentes/skills en `1e03577`; State inicial en `c8e2bec`; autoevaluación completa y períodos de Secretaría en `8df88de`. Integra `origin/anton` hasta `f0b18b0`, preservando sus cambios y las nuevas pruebas institucionales. Incluye V6–V8, textos, porcentajes, permisos y acceso por áreas solo durante períodos abiertos. |
| `backend` | Java 21, Spring Boot 4.1.1, PostgreSQL, JPA, Flyway, validación y Spring Security. Configuración de BD y JWT mediante variables de entorno o perfil `local` ignorado por Git. V4 crea las tablas del catálogo; V5 agrega el borrador anual y valoraciones por componente; V6 carga el inventario oficial de 2026. El arranque con PostgreSQL local y `ddl-auto=validate` fue verificado. |
| `frontend` | Vue 3, TypeScript, Vite y router. En `JuanM` local hay acceso de institución/SEM, pantalla de sesión y primera vista de borrador anual. La vista muestra el catálogo cuando tenga componentes activos. La pantalla del PMI sigue pendiente. |

Los controladores de catálogo de `anton` exponen creación y consulta de áreas, procesos y componentes. Su implementación cambia con la rama; verificar código y contratos actuales antes de trabajar sobre ella. La rama `main` no contiene todavía estos avances.

## Primera iteración del borrador anual en `JuanM`

- `PUT /api/self-evaluations/{year}` crea o recupera el borrador de la institución autenticada. `GET /api/self-evaluations/{year}` lo consulta y `GET /api/self-evaluations` lista sus años. Las consultas admiten institución y Secretaría; Secretaría debe indicar `?establishmentId=<UUID de institución>`. Crear y guardar valoraciones siguen siendo acciones de la institución sobre sus propios registros.
- `PUT /api/self-evaluations/{year}/valuations/{componentId}` guarda nivel 1-4 y, opcionalmente en el borrador, un enlace HTTP(S), una nota de evidencia, `strengths` e `improvementOpportunities`. V7 agrega los dos textos institucionales sin alterar registros anteriores. Solo acepta componentes activos.
- Aptitud confirmada por Juan el 6 de octubre: Existencia permite solo oportunidades; Pertinencia y Apropiación permiten fortalezas y oportunidades; Mejoramiento continuo permite solo fortalezas. State expone `allowsStrengths()` y `allowsImprovementOpportunities()`; la API devuelve sus indicadores y rechaza textos prohibidos con `400`, sin alterar la valoración. Vue deshabilita campos no permitidos y ofrece quitar un texto incompatible antes de guardar. La clasificación del diagrama para candidatos del PMI se conserva por separado.
- La institución elige manualmente Existencia, Pertinencia, Apropiación o Mejoramiento continuo. El backend aplica State mediante `ValuationState`, `ComponentValue` y cuatro clases concretas; cada estado identifica su código/etiqueta y aporta una unidad a su subtotal. Según el diagrama de clases, `isImprovementChance()` devuelve verdadero en Existencia/Pertinencia y `isStrength()` en Apropiación/Mejoramiento continuo. Se mantienen los códigos 1–4 en BD y API para conservar los registros existentes.
- La consulta del borrador añade `totals` por estado, total anual de valoraciones guardadas y `totals.percentages`. Por solicitud de Juan, el espacio de la institución presenta cuatro entradas de autoevaluación por área. `/autoevaluacion/:areaId` muestra únicamente sus procesos, componentes, conteos y porcentajes, con un resumen del área y subtotales por proceso. Se agregaron estilos modestos para tablas y encabezados de procesos, autorizados por Juan mientras se define el manual de imagen. Fuente funcional: revisión directa del Anexo 1 del Excel local el 6 de octubre y confirmación de que el formato se conserva con el catálogo reducido de 2026. Detalles en [valoración de componentes](valoracion-componentes.md).
- La implementación supone un borrador por institución y año, protegido por una restricción única en PostgreSQL. La guía describe un ciclo anual; **la unicidad exacta y los estados posteriores** son decisiones de producto que debe confirmar el equipo.
- Fuente funcional: Guía 34 de 2026, etapa de autoevaluación (pp. 49-74) y matriz de valoración con evidencias (anexo 2, pp. 164-167). El diagrama de flujo de la wiki propone el diligenciamiento por componentes y el envío posterior a revisión.
- V6 agrega la carga inicial de 4 áreas, 15 procesos y 45 componentes activos, contrastada con el anexo 2 (pp. 164-167). Detalle e instrucciones en [catálogo de la Guía 34](catalogo-guia34-2026.md). Los descriptores por nivel del anexo 1 todavía no están modelados en la aplicación.

## Período anual habilitado por Secretaría

Juan confirmó el 6 de octubre de 2026 que Secretaría configura el inicio y fin y que los borradores se crean automáticamente para todas las instituciones registradas. V8 incorpora `PeriodRecord`, `SelfEvaluationPeriod` y la relación anual. La API permite a Secretaría publicar o ajustar fechas; las instituciones solo pueden crear o guardar dentro del período, incluyendo ambos días según America/Bogota. Fuera del período conservan la consulta. La interfaz agrega **Administrar períodos** en Secretaría y muestra las fechas/estado en la institución. Los borradores anteriores se conservan, sin fechas publicadas automáticamente. Detalles en [períodos de autoevaluación](periodos-autoevaluacion.md).

Secretaría también dispone de **Eliminar período**, con confirmación en pantalla y `DELETE /api/self-evaluation-periods/{year}`. Retira las fechas y cierra la escritura institucional; conserva autoevaluaciones y respuestas. Puede volver a habilitar el mismo año sin duplicar borradores. Solicitud de Juan del 6 de octubre de 2026; utiliza la relación nullable de V8.

Juan aclaró que las autoevaluaciones institucionales deben aparecer solo cuando Secretaría habilite el diligenciamiento: las cuatro áreas se muestran únicamente para períodos abiertos publicados. Se retiraron el año libre y la creación manual del borrador en la interfaz de institución. Los enlaces directos también comprueban el período antes de cargar respuestas o componentes. La API conserva los registros y sus permisos de consulta; la pantalla ya no expone formularios fuera del período abierto.

## Decisiones por confirmar con el equipo

- Los comentarios de Secretaría deben permanecer separados de las respuestas institucionales. Su registro y el flujo formal de revisión siguen pendientes.

- Qué versión de la Guía 34 y qué instrucciones propias de la ciudad/SEM prevalecen si difieren del PDF de 2026 enlazado en la wiki.
- El diagrama de flujo propone priorizar hasta dos componentes y seleccionar por puntaje de urgencia/tendencia/impacto. Contrastarlo con la metodología de la guía, que parte de oportunidades por las cuatro áreas (p. 77), antes de fijarlo en datos o validaciones.
- Quién puede crear o editar el catálogo de áreas, procesos y componentes, y cómo se publica/versiona para cada año escolar.
- Estados y permisos de borrador, envío, observación, corrección y cierre de autoevaluaciones/PMI; reglas de unicidad por institución y periodo.
- El diagrama confirma como candidatos los estados 1 y 2 y propone priorizar por urgencia/tendencia/impacto. El subtotal describe la distribución; la priorización requiere mantener las valoraciones individuales y valorar U/T/I (Guía 34, pp. 77-80). `PrioritaryComponent` y la selección posterior aún están pendientes.
- Contrato para evidencias: hoy el flujo dibuja enlaces y notas sobre ubicación; definir almacenamiento, acceso, privacidad y conservación antes de implementar cargas de archivos.

Actualizar esta nota cuando se integren ramas, cambie la wiki o el equipo cierre las decisiones anteriores.

La correspondencia entre el código actual y las clases del diagrama, incluidos los elementos pendientes, está en [alineación con el diagrama de clases](alineacion-diagrama-clases.md).
