# Estado y fuentes del proyecto

Revisión: 28 de octubre de 2026. Esta nota resume lo que se pudo leer del repositorio, su wiki, dos diagramas adjuntos y la Guía 34 enlazada. No reemplaza los artefactos originales.

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
| `origin/anton` | Commit `ff6d49b`, consultado el 21 de octubre de 2026. Incorpora `JuanM` hasta `9bd3ae4` y añade cuatro commits de relaciones inversas JPA, aclaraciones funcionales, tipografía y colores. Conserva las operaciones de actualización, activación/desactivación y cambio de padre de los catálogos. |
| `JuanM` | Borrador e interfaz inicial en `e29f155`; limpieza de agentes/skills en `1e03577`; State inicial en `c8e2bec`; autoevaluación completa y períodos de Secretaría en `8df88de`. Integra `origin/anton` hasta `ff6d49b`. Corrige `SelfEvaluation.valuations` a `mappedBy = "evaluation"`, que es el atributo propietario en `ComponentValuation`, y verifica la navegación de las relaciones persistidas. Incluye V6–V8, textos, porcentajes, permisos y acceso por áreas solo durante períodos abiertos. |
| `backend` | Java 21, Spring Boot 4.1.1, PostgreSQL, JPA, Flyway, validación y Spring Security. Configuración de BD y JWT mediante variables de entorno o perfil `local` ignorado por Git. V4 crea las tablas del catálogo; V5 agrega el borrador anual y valoraciones por componente; V6 carga el inventario oficial de 2026. El arranque con PostgreSQL local y `ddl-auto=validate` fue verificado. |
| `frontend` | Vue 3, TypeScript, Vite y router. Acceso de institución/SEM, períodos y autoevaluación por áreas. La primera etapa del PMI dispone de matriz de factores críticos y U/T/I, ranking y selección de componentes desde las autoevaluaciones guardadas, con la paleta SEM de Antonio. |

Los controladores de catálogo de `anton` exponen creación y consulta de áreas, procesos y componentes. Su implementación cambia con la rama; verificar código y contratos actuales antes de trabajar sobre ella. La rama `main` no contiene todavía estos avances.

## Primera iteración del borrador anual en `JuanM`

- `PUT /api/self-evaluations/{year}` crea o recupera el borrador de la institución autenticada. `GET /api/self-evaluations/{year}` lo consulta y `GET /api/self-evaluations` lista sus años. Las consultas admiten institución y Secretaría; Secretaría debe indicar `?establishmentId=<UUID de institución>`. Crear y guardar valoraciones siguen siendo acciones de la institución sobre sus propios registros.
- `PUT /api/self-evaluations/{year}/valuations/{componentId}` guarda nivel 1-4 y, opcionalmente, una referencia de evidencia (enlace o texto), una nota adicional, `strengths` e `improvementOpportunities`. V7 agrega los dos textos institucionales y V13 amplía la referencia a texto de hasta 10.000 caracteres, conservando el nombre JSON `evidenceUrl` por compatibilidad y los enlaces anteriores. Solo acepta componentes activos de áreas no enviadas.
- Aptitud confirmada por Juan el 6 de octubre: Existencia permite solo oportunidades; Pertinencia y Apropiación permiten fortalezas y oportunidades; Mejoramiento continuo permite solo fortalezas. State expone `allowsStrengths()` y `allowsImprovementOpportunities()`; la API devuelve sus indicadores y rechaza textos prohibidos con `400`, sin alterar la valoración. Vue deshabilita campos no permitidos y ofrece quitar un texto incompatible antes de guardar. La clasificación del diagrama para candidatos del PMI se conserva por separado.
- La institución elige manualmente Existencia, Pertinencia, Apropiación o Mejoramiento continuo. El backend aplica State mediante `ValuationState`, `ComponentValue` y cuatro clases concretas; cada estado identifica su código/etiqueta y aporta una unidad a su subtotal. Según el diagrama de clases, `isImprovementChance()` devuelve verdadero en Existencia/Pertinencia y `isStrength()` en Apropiación/Mejoramiento continuo. Se mantienen los códigos 1–4 en BD y API para conservar los registros existentes.
- La consulta del borrador añade `totals` por estado, total anual de valoraciones guardadas y `totals.percentages`. Por solicitud de Juan, el espacio de la institución presenta cuatro entradas de autoevaluación por área. `/autoevaluacion/:areaId` muestra únicamente sus procesos, componentes, conteos y porcentajes, con un resumen del área y subtotales por proceso. Se agregaron estilos modestos para tablas y encabezados de procesos, autorizados por Juan mientras se define el manual de imagen. Fuente funcional: revisión directa del Anexo 1 del Excel local el 6 de octubre y confirmación de que el formato se conserva con el catálogo reducido de 2026. Detalles en [valoración de componentes](valoracion-componentes.md).
- La implementación supone un borrador por institución y año, protegido por una restricción única en PostgreSQL. La guía describe un ciclo anual; **la unicidad exacta y los estados posteriores** son decisiones de producto que debe confirmar el equipo.
- Fuente funcional: Guía 34 de 2026, etapa de autoevaluación (pp. 49-74) y matriz de valoración con evidencias (anexo 2, pp. 164-167). El diagrama de flujo de la wiki propone el diligenciamiento por componentes y el envío posterior a revisión.
- V6 agrega la carga inicial de 4 áreas, 15 procesos y 45 componentes activos, contrastada con el anexo 2 (pp. 164-167). Detalle e instrucciones en [catálogo de la Guía 34](catalogo-guia34-2026.md). Los descriptores por nivel del anexo 1 todavía no están modelados en la aplicación.

## Período anual habilitado por Secretaría

Juan confirmó el 6 de octubre de 2026 que Secretaría configura el inicio y fin y que los borradores se crean automáticamente para todas las instituciones registradas. V8 incorpora `PeriodRecord`, `SelfEvaluationPeriod` y la relación anual. La API permite a Secretaría publicar o ajustar fechas; las instituciones solo pueden crear o guardar dentro del período, incluyendo ambos días según America/Bogota. Fuera del período conservan la consulta. La interfaz agrega **Administrar períodos** en Secretaría y muestra las fechas/estado en la institución. Los borradores anteriores se conservan, sin fechas publicadas automáticamente. Detalles en [períodos de autoevaluación](periodos-autoevaluacion.md).

Secretaría también dispone de **Eliminar período**, con confirmación en pantalla y `DELETE /api/self-evaluation-periods/{year}`. Por aclaración expresa de Juan del 24 de octubre, elimina también los PMI de todas las áreas e instituciones de ese año, incluidos los anexos 2–5 y su historial de seguimiento. Las respuestas y envíos quedan para consulta hasta volver a publicar ese año; al publicarlo nuevamente, se reinician vacíos por la aclaración del 26 de octubre. El registro anual se reutiliza sin duplicarse. Mientras no se publique otra vez el período, el panel oculta ese PMI y la API impide recrearlo. Editar fechas o llegar a la fecha de cierre mantiene las respuestas, envíos y PMI.

Juan aclaró que las autoevaluaciones institucionales deben aparecer solo cuando Secretaría habilite el diligenciamiento: las cuatro áreas se muestran únicamente para períodos abiertos publicados. Se retiraron el año libre y la creación manual del borrador en la interfaz de institución. Los enlaces directos también comprueban el período antes de cargar respuestas o componentes. La API conserva los registros y sus permisos de consulta; la pantalla ya no expone formularios fuera del período abierto.

**Aclaración del 26 de octubre:** al eliminar y volver a publicar el mismo año, las cuatro áreas deben empezar vacías. La nueva publicación limpia valoraciones y sus textos/evidencias, envíos y cualquier PMI residual de ese año antes de vincular los borradores. Reemplaza la conservación anterior de respuestas al volver a publicar. Editar fechas de un período que todavía existe conserva sus respuestas y PMI. No se alteran otros años ni se reinician datos al arrancar el servidor; el cambio actúa al publicar un período que no existe.

## Ayuda de componentes y evidencias

La ayuda **Ayuda del componente** incorpora una descripción breve de qué evalúa cada uno de los 45 componentes y dos ejemplos de evidencia pertinentes, con fuente y páginas. Juan pidió solo la descripción del ítem, sin los cuatro descriptores de estado. El anexo 3 oficial organiza fuentes por proceso; su selección por componente es orientativa, no una exigencia documental. Las diez ayudas administrativas/financieras se diferenciaron según el componente. El campo de evidencia admite texto para referir documentos físicos sin exigir escaneo y conserva el guardado automático. Reglas y fuente en [ayuda de componentes y evidencias](ayuda-componentes-evidencias.md).

El 26 de octubre, por solicitud de Juan, se retiró de la interfaz el segundo recuadro de nota: cada componente presenta solo **Evidencias: enlaces o descripción**. Los enlaces/descripciones y las notas anteriores se muestran juntos sin escritura al cargar; al guardar se conserva el contenido en `evidenceUrl` y se limpia la nota legada para evitar duplicados. El límite de referencia aumenta a 20.002 caracteres para conservar dos textos anteriores completos; la API mantiene `evidenceNote` por compatibilidad. No requiere migración ni cambia los permisos del área enviada.

## Envío por área y guardado automático

Juan solicitó un botón de envío independiente por área y guardado automático. V12 incorpora `SelfEvaluationAreaSubmission`, único por autoevaluación/área, con fecha persistida. Completar los niveles ya no habilita automáticamente el PMI: debe pulsarse **Enviar autoevaluación de esta área** dentro del período abierto. El servidor verifica completitud y conserva el envío de forma idempotente. Las respuestas del área enviada quedan para consulta, mientras las otras áreas siguen editables; cambiar o retirar el período no elimina el envío.

Vue guarda cada respuesta unos 800 ms después de dejar de editar, mantiene la última versión si hay una petición en curso y permite reintentar errores. Antes de enviar espera y guarda los cambios pendientes de toda el área; si hay un fallo no ejecuta el envío. Las tarjetas del PMI distinguen completa de enviada. Los borradores existentes no se marcan enviados por migración. El registro anual conserva su estado `DRAFT`; el estado de entrega pertenece al área. Contrato y límites en [envío de autoevaluación por área](envio-autoevaluacion-por-area.md).

## Primera etapa del PMI: Anexo 2

Por solicitud de Juan del 21 de octubre, el Anexo 2 del Excel de evidencias implementa análisis y priorización de factores críticos. V9 agrega `PMI` y `PrioritaryComponent`; V11 separa el PMI por autoevaluación anual y área. La institución describe factores y asigna U/T/I enteros de 1 a 5; el servidor calcula la suma. La aclaración aprobada de Juan prevalece sobre la selección inicial del diagrama: completar y enviar explícitamente la autoevaluación del área habilita su PMI, con candidatos únicamente del menor estado presente (Existencia, o Pertinencia en su ausencia, o Apropiación si no hay ninguno en los dos anteriores). Si todos están en Mejoramiento continuo no hay candidatos. Esta elegibilidad depende del contexto del área, sin modificar los estados de valoración históricos.

Después de puntuar todos los candidatos del área, pasan al Anexo 3 los de mayor U/T/I: uno si el máximo es único, ambos si hay dos, o exactamente dos elegidos por la institución entre tres o más empatados. No hay umbral fijo de 15 ni adicional con puntaje inferior. Los cambios de origen conservan análisis y objetivos previos; los que dejan de ser elegibles son históricos para consulta. La API permite consulta propia y de Secretaría con selector institucional, pero solo la institución escribe. El PMI no queda bloqueado por el cierre del período de autoevaluación; un calendario propio y el cierre formal siguen pendientes de definición.

La entrada institucional del PMI muestra tarjetas por año y área, avance valorados/total y habilitación tras completar y enviar cada área. `/pmi/:year` resume sus áreas y `/pmi/:year/areas/:areaId` abre su Anexo 2. Los estilos utilizan las variables SEM y la tipografía de Antonio; las hojas globales se conservan. Trazabilidad, contrato y pasos de prueba en [PMI: Anexo 2](pmi-anexo2.md).

## Formulación del PMI: Anexo 3

Juan solicitó continuar con la hoja Anexo 3 del mismo Excel. V10 incorpora objetivos ligados a los componentes priorizados, metas y filas con nombre de indicador, bloque de actividades, costo, fuentes FSE/otra, responsable, inicio/fin e instrumentos/frecuencia de recolección. El formato permite varios indicadores por meta; se amplía la relación singular del diagrama mediante filas de actividad/indicador. Un objetivo por área y hasta cuatro actividades se presentan como orientación, sin imponer restricciones no confirmadas.

La institución guarda borradores parciales por objetivo en `/pmi/:year/areas/:areaId/formulacion`, enlazado desde Anexo 2. `canFormulate` exige completar y enviar la autoevaluación del área, puntuar todos sus candidatos y resolver los máximos. Solo los componentes seleccionados y elegibles permiten crear/editar; los objetivos que pierden esa condición se conservan para consulta y solo se eliminan por acción explícita. Secretaría dispone de consulta por API con selector institucional. Los importes usan decimales exactos y distinguen vacío de cero; los totales se limitan al área, excluyen objetivos históricos y señalan campos pendientes. Se conserva la paleta de Antonio. La ficha detallada se diligencia en Anexo 4 y el avance de ejecución en Anexo 5; la medición numérica de las fórmulas y la revisión formal siguen pendientes. Fuente, contrato, límites técnicos y pruebas en [PMI: Anexo 3](pmi-anexo3.md).

## Interfaz del Anexo 3 para docentes

La simplificación del Anexo 3 solicitada por Juan el 23 de octubre incorpora dos pasos: tarjetas de componentes con mayor U/T/I y **Generar formularios**, seguido del plan de cada componente con todos sus espacios iniciales preparados. Conserva la regla de uno o dos máximos y permite elegir dos cuando hay más empatados. Reutiliza datos e IDs, conserva históricos y exige guardar las respuestas por plan. La selección del Anexo 3 utiliza la misma API de priorización; no cambia las entidades ni las reglas de permisos. Detalles en [Anexo 3](pmi-anexo3.md).

## Ficha de indicadores: Anexo 4

Juan solicitó el 24 de octubre continuar con el Anexo 4 del Excel: una tabla independiente por cada indicador guardado con nombre en el Anexo 3. V14 amplía el mismo `Indicator` con tipo proceso/resultado, objetivo, unidad, variables, fórmula, fuentes, metodología, periodicidad y responsable de medición. El nombre se consulta del origen, de modo que renombrar la misma fila conserva su ficha. Permite borradores y guardado individual; las fórmulas son texto, sin ejecutar cálculos de seguimiento. Se mantienen la pertenencia institucional, el PMI por área/año y las restricciones de selección vigentes; los históricos siguen para consulta y Secretaría solo puede leer por API con selector institucional. La pantalla `/pmi/:year/areas/:areaId/indicadores` se abre desde Anexo 3 y conserva la paleta SEM. Fuente, contrato y límites en [PMI: Anexo 4](pmi-anexo4.md).

## Seguimiento del PMI: Anexo 5

El 24 de octubre, Juan solicitó continuar con el Anexo 5 del Excel. Se toma como referencia la hoja de 2026 y se conserva una fila por indicador/bloque de actividades del Anexo 3 del área. V15 incorpora `PMIFollowUp`: fecha de control, estado manual NI/ESP/CANC/FIN/EJ, porcentaje 0–100 y observaciones institucionales con referencias de evidencia. Se guardan controles independientes por fecha y una instantánea del contexto al registrar cada fila; cambiar o eliminar el origen no altera esos controles. La institución escribe únicamente sobre filas seleccionadas/elegibles y Secretaría consulta por API con selector. `/pmi/:year/areas/:areaId/seguimiento`, accesible desde Anexos 3 y 4, mantiene la paleta SEM y guardado individual. No automatiza el cálculo de la fórmula del indicador, la revisión de Secretaría ni el envío/cierre del PMI. Contrato y trazabilidad en [PMI: Anexo 5](pmi-anexo5.md).

La corrección del Anexo 5 confirmada por Juan el 26 de octubre establece consulta durante la formulación y diligenciamiento del PMI del año anterior cuando se abra el período siguiente. La autoevaluación nueva queda bloqueada en pantalla y API hasta finalizar todos los seguimientos previos de la institución. Para finalizar son obligatorios estado, porcentaje y observaciones de cada fila; V16 incorpora su registro persistente. El consolidado muestra proceso, componente, objetivo y meta con cada indicador una sola vez. No exige 100 % de ejecución; exige informar el resultado real. Las finalizaciones se revalidan si cambia el plan. Detalles en [Anexo 5](pmi-anexo5.md).

El 28 de octubre, por solicitud de Juan, el consolidado institucional del Anexo 5 adopta una tabla similar al Excel: contexto agrupado, plazos, cinco columnas de estado, porcentaje y observaciones/evidencias. Durante formulación estos últimos espacios se muestran vacíos y bloqueados; solo se habilitan para el seguimiento autorizado del período siguiente. Se conserva la consulta después del cierre o finalización, el guardado por fila, la paleta SEM y el desplazamiento horizontal dentro de la tabla en celular.

## Paleta visual compartida

El 23 de octubre, por solicitud de Juan, se unificaron los estilos que todavía conservaban verdes de prueba con la paleta SEM definida por Antonio en `frontend/src/assets/base.css`. El panel, las áreas de autoevaluación, los encabezados de procesos, las tablas y los períodos de Secretaría utilizan azul y neutros de esas variables; rojo y amarillo conservan sus funciones de error/aviso y foco. Se verificaron compilación Vue/TypeScript y ESLint y se revisaron acceso, panel, autoevaluación y Secretaría en navegador con datos aislados.

El 28 de octubre se adaptan las pantallas a los 18 PNG de **SEM Mockups** suministrados por Juan: acceso por rol, barra azul y menú lateral, inicio con tarjetas, datos de cuenta en consulta, autoevaluación y revisión en columnas de escritorio, historial/paquetes con búsqueda por nombre o DANE y definición de fechas. Se mantienen los colores, tipografía, evidencia única, envío/PMI por área, comentarios y seguimiento obligatorio. Las diferencias entre los bocetos y las funciones disponibles, los componentes compartidos y la validación están en [diseño de los mockups SEM](diseno-mockups-sem.md).

## Envío explícito del PMI

**Envío de PMI confirmado el 28 de octubre:** Juan solicitó sustituir la consulta automática de avances por **Finalizar y enviar PMI del área**. V19 agrega la fecha de envío; los registros previos siguen como borradores privados. Se validan anexos 2–4 y se bloquea su edición tras enviar. Secretaría consulta solo PMI enviados en bandeja y API; editar borradores no invalida revisiones del paquete. El seguimiento del Anexo 5 se conserva para el período siguiente sin exigirlo en esta entrega. Contrato y validaciones en [envío del PMI por área](envio-pmi-por-area.md).

## Decisiones por confirmar con el equipo

- Los comentarios de Secretaría permanecen separados de las respuestas institucionales. Su registro por componente y la marca de paquete revisado están implementados; una aprobación/rechazo o reapertura formal no están definidos.

- Qué versión de la Guía 34 y qué instrucciones propias de la ciudad/SEM prevalecen si difieren del PDF de 2026 enlazado en la wiki.
- El diagrama de flujo propone priorizar hasta dos componentes y seleccionar por puntaje de urgencia/tendencia/impacto. Contrastarlo con la metodología de la guía, que parte de oportunidades por las cuatro áreas (p. 77), antes de fijarlo en datos o validaciones.
- Quién puede crear o editar el catálogo de áreas, procesos y componentes, y cómo se publica/versiona para cada año escolar.
- Estados y permisos de borrador, envío, observación, corrección y cierre de autoevaluaciones/PMI; reglas de unicidad por institución y periodo.
- La aclaración aprobada por Juan reemplaza la regla inicial del diagrama para candidatos y selección: mínimo 1/2/3 por área y hasta dos máximos U/T/I. Anexo 4 incorpora la ficha detallada y Anexo 5 el seguimiento de ejecución; la medición numérica de las fórmulas y la revisión formal siguen pendientes.
- Contrato para evidencias: hoy el flujo dibuja enlaces y notas sobre ubicación; definir almacenamiento, acceso, privacidad y conservación antes de implementar cargas de archivos.

### Respuestas a dudas estipuladas:
- **¿Qué versión de la Guía 34 y qué instrucciones propias de la ciudad/SEM prevalecen si difieren del PDF de 2026 enlazado en la wiki.?**
  La guía 34 es nuestro origen de la información y como hacer las cosas, sin embargo, si nos dicen hacer algo de cierta manera desde la sub secretaría de calidad, entonces la regla de negocio cambia para satisfacer dichas necesidades que nos estipulan allá.
- **El diagrama de flujo propone priorizar hasta dos componentes y seleccionar por puntaje de urgencia/tendencia/impacto. Contrastarlo con la metodología de la guía, que parte de oportunidades por las cuatro áreas (p. 77), antes de fijarlo en datos o validaciones.**
  El flujo de priorización de componentes es como nos lo han planteado desde la Secretaria de educación, su comportamiento tal y como se lo plantea de manera lógica es la interpretación mas cercana a las instrucciones dadas por la secretaría de educación.
- **¿Quién puede crear o editar el catálogo de áreas, procesos y componentes, y cómo se publica/versiona para cada año escolar?**
  Nos indican por un lado que toda la parte de gestión integral puede que no sufra cambios de aquí a muchos años, sin embargo, pensándolo de manera escalable no hay un consenso claro de quien es el que va cambiar dicho catalogo, apuntando vagamente al director de sistemas de información de la secretaria de educación. Por el momento tengo la intención de dejar creación y edición de gestión integral a los usuarios del tipo secretaría.
- **Estados y permisos de borrador, envío, observación, corrección y cierre de autoevaluaciones/PMI; reglas de unicidad por institución y periodo.**
  Recuerdo lo siguiente: las autoevaluaciones son un ejercicio moral, y aunque viene bien tener los siguientes estados: borrador, envío, observación y cierre. Cabe aclarar que cuando se realizan las observaciones por parte del personal de secretaría, ellos no esperan que la institución modifique su autoevaluación actual para cumplir con dichas observaciones, sino que tomen nota para realizarlas en la siguiente entrega.
- **Contrato para evidencias: hoy el flujo dibuja enlaces y notas sobre ubicación; definir almacenamiento, acceso, privacidad y conservación antes de implementar cargas de archivos.**
  Nosotros no manejamos archivos de manera propia, ni pretendemos hacerlo. Se nos estipula por parte de la secretaría de educación que las instituciones están acostumbradas a utilizar repositorios de archivos como Google Drive y prefieren mantener dichos repositorios para las entregas, a pesar de los problemas de trazabilidad que esto pueda traer o lo ineficiente que sea, esto es una decisión que nos dejaron claro prefieren mantenerlo de esta forma. Por ello, lo único que se debe solicitar es un link de acceso general hacia el repositorio con la nota que especifique la ubicación de las evidencias en las carpetas del repositorio.

Actualizar esta nota cuando se integren ramas, cambie la wiki o el equipo cierre las decisiones anteriores.

## Revisión de Secretaría e historial (27 de octubre de 2026)

Juan confirmó consulta institucional por año/área y revisión de Secretaría sin editar respuestas. Su corrección final limita las observaciones a cada componente de la autoevaluación: el PMI se consulta sin comentarios de Secretaría. La bandeja muestra solo áreas enviadas y el avance guardado de sus PMI. V17 incorpora `ComponentReviewComment`, separado de la valoración, con autor y fecha; los comentarios orientan la siguiente entrega, sin aprobación, rechazo o reapertura automática.

Su siguiente solicitud agrupa las entregas en **paquetes por institución y año**, con nombre, DANE, áreas recibidas y PMI juntos. V18 agrega `DeliveryPackageReview`: Secretaría puede marcar el paquete como revisado o devolverlo a pendiente; se conserva autor y fecha de revisión. Nuevas áreas o cambios guardados en PMI vuelven pendiente una revisión anterior y se rechaza marcar una versión desactualizada. La marca no cambia las respuestas ni sustituye el seguimiento previo. La institución ve únicamente sus propios paquetes y su estado de revisión.

La institución dispone de **Mis autoevaluaciones e historial**, independiente del período abierto y del requisito de seguimiento. Puede consultar sus respuestas anteriores, comentarios y Anexos 2–5; el aviso de seguimiento enlaza también la autoevaluación anterior. Abrir un año nuevo no reinicia el anterior. Se mantiene la decisión previa de reiniciar respuestas/envíos al eliminar y volver a publicar **el mismo año**, limpiando también las observaciones ligadas a esa entrega anterior. Detalles, contratos y trazabilidad en [revisión e historial](revision-secretaria-historial.md).

## Integración del 21 de octubre de 2026

Se incorporan `46e5aab`, `de419f9`, `5382e4e` y `ff6d49b` en `JuanM`. Las relaciones inversas institución → autoevaluaciones, autoevaluación → valoraciones y componente → valoraciones reutilizan las claves foráneas existentes y no requieren migración. Se corrige el `mappedBy` de autoevaluación, que apuntaba al nombre de tabla en vez del atributo Java. La prueba de persistencia recarga las entidades desde PostgreSQL y comprueba sus tres relaciones.

Se conservan las aclaraciones de Antonio sobre revisión para la siguiente entrega y evidencias por enlaces externos, junto con sus estilos de acceso y variables de color SEM. Los comentarios TODO de autenticación describen pendientes; no implementan registro ni recuperación de contraseña.

La correspondencia entre el código actual y las clases del diagrama, incluidos los elementos pendientes, está en [alineación con el diagrama de clases](alineacion-diagrama-clases.md).

## Integración del 28 de octubre de 2026

Por autorización de Juan se integran en `JuanM` los commits `3059c84`, `f75f3d0`, `4800de5` y `bf70ef9` de `anton`. Se incorporan registro de instituciones y Secretaría por SEM, actualización de la cuenta autenticada, estilos reutilizables, `AppTopBar`, `SvgIcon`, logo, ondas e iconos. Se resuelven los conflictos de acceso y panel conservando los flujos de cuatro áreas, seguimiento obligatorio, paquetes y comentarios de JuanM y reutilizando los recursos de Antonio.

La interfaz añade **Secretaría → Usuarios** y **Datos de la cuenta → Editar datos** con sus API reales. Editar y guardar cierra la sesión actual; los errores y la cancelación conservan el formulario. Las instituciones creadas durante períodos abiertos reciben borradores vacíos en la misma transacción, sin modificar datos de otras instituciones. Se ajustan límites de registro al esquema existente y la actualización recarga al usuario antes de aplicar el PATCH. No se agregan migraciones. Se verifican 160 pruebas de backend, compilación/ESLint y recorridos de navegador. Detalles en [integración de Antonio](integracion-anton-2026-10-28.md).
