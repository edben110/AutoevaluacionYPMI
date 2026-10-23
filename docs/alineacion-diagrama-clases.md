# Alineación con los diagramas del equipo

Revisión del 6 de octubre de 2026. Fuentes: [diagrama de clases](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-Diagrama-de-clases) y [diagrama de flujo](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-diagrama-de-flujo). Se leyeron las etiquetas de los SVG originales enlazados en la wiki; las copias locales están en `.codex/references/`, ignoradas por Git.

## Modelo actual y trabajo pendiente

| Clase del diagrama | Código actual | Correspondencia y pendientes |
| --- | --- | --- |
| `User`, `Establishment`, `Secretary`, `EmailRecord` | Mismas clases existentes de `anton` | Se conserva el modelo de usuarios. Los permisos y consultas se implementan en controlador/servicios, manteniendo las entidades para persistencia. |
| `IntegralGestion`, `Area`, `Process`, `Component` | `IntegralManagement`, `Area`, `Process`, `Component` | Se conserva la base y las relaciones área → proceso → componente. El catálogo V6 agrega el inventario oficial. |
| `SelfAssessment` | `SelfEvaluation` | Una institución, año, colección de valoraciones y relación al período anual que contiene `PeriodRecord`. `valuations` es la relación inversa de `ComponentValuation.evaluation`, incorporada el 21 de octubre de 2026 junto con las colecciones de autoevaluaciones por institución y valoraciones por componente. El año se guarda como entero; la colección/snapshot de áreas del diagrama sigue pendiente. |
| `ComponentValuation` | `ComponentValuation` | Referencia al componente, autoevaluación, estado y enlace de evidencia. `proofLink` corresponde a `evidenceUrl`. `evidenceNote` es la nota de ubicación de evidencia de la institución; no sustituye las observaciones de Secretaría del diagrama. |
| `ComponentValue` | `ComponentValue` | Enum con los cuatro estados; se conserva la codificación 1–4 de la BD/API y nombres con ortografía normalizada. |
| Interfaz `ValueState` y cuatro estados | `ValuationState` y cuatro estados | `getValue()`, `isImprovementChance()` e `isStrength()` implementados. Conteo por estado delegado al objeto State. El nombre de la interfaz difiere; su función corresponde al diagrama. |
| `PrioritaryComponent` | `pmi.entity.PrioritaryComponent` | Referencia `ComponentValuation`, guarda factores críticos institucionales, U/T/I y posición de selección. `getSumUTI()` calcula la suma. Las observaciones de Secretaría siguen pendientes y separadas de estos textos. |
| `PMI` | `pmi.entity.PMI` y `PMIService` | Anexos 2 y 3 con vínculo a autoevaluación y área, proyección de un año, fecha y metadatos. V11 sustituye el vínculo anual único por autoevaluación+área y preserva formulaciones existentes. La aclaración de Juan exige autoevaluación del área completa y enviada, candidatos del mínimo 1/2/3 y hasta dos máximos U/T/I. Esta regla contextual amplía el diagrama y no depende exclusivamente de `isImprovementChance()`. La ventana propia del PMI sigue pendiente. |
| `Objective`, `Goal`, `Activity`, `Indicator` | `pmi.entity` y `PMIFormulationService` | V10 vincula objetivo a componente priorizado, meta a objetivo y filas de actividades a meta. Cada fila contiene un indicador con nombre y su bloque de actividades, recursos, responsable, fechas y recolección. El Excel del Anexo 3 tiene varios indicadores por meta: se amplía la relación singular del diagrama mediante estas filas, sin dividir automáticamente bloques de actividades. La ficha detallada de indicadores y los resultados de seguimiento siguen pendientes. Véase [Anexo 3](pmi-anexo3.md). |
| `PeriodRecord` | `shared.value.PeriodRecord` | Inicio, fin, inclusión de fecha y duración con ambos extremos incluidos. Embebido en `SelfEvaluationPeriod`, publicado por Secretaría para el año y vinculado a los borradores. |

## State según el diagrama

| Estado | `isImprovementChance()` | `isStrength()` |
| --- | --- | --- |
| Existencia | true | false |
| Pertinencia | true | false |
| Apropiación | false | true |
| Mejoramiento continuo | false | true |

La institución selecciona el estado manualmente. Los indicadores booleanos se derivan del objeto State y preparan la identificación posterior de oportunidades y fortalezas. La clasificación es una decisión del diseño del equipo, confirmada por sus dos diagramas.

Como extensión del diligenciamiento confirmado por Juan tras revisar el Excel, `ComponentValuation` guarda `strengths` e `improvementOpportunities` por separado. No son observaciones de Secretaría. Juan aclaró el 6 de octubre de 2026 la aptitud: Existencia permite solo oportunidades; Pertinencia y Apropiación permiten ambos textos; Mejoramiento continuo permite solo fortalezas. Los campos permitidos siguen siendo opcionales en borrador.

`allowsStrengths()` y `allowsImprovementOpportunities()` agregan esta aptitud al State y la API expone `strengthsAllowed` e `improvementOpportunitiesAllowed`. La clasificación de la tabla anterior sigue correspondiendo al diagrama para el análisis de componentes críticos; la aptitud de escritura se documenta como una regla adicional confirmada por Juan. El backend rechaza combinaciones inválidas antes de modificar los datos y Vue permite quitar explícitamente textos incompatibles con el nuevo estado antes de guardar.

## Permisos de autoevaluación

El flujo muestra a Secretaría eligiendo una institución, recuperando su autoevaluación y revisando componentes/evidencias. Los permisos se colocan por método en `SelfEvaluationController`, sin bloquear el controlador completo al rol `SECRETARY`.

| Operación | Institución | Secretaría |
| --- | --- | --- |
| Listar autoevaluaciones y consultar un año | Sus propios registros | Registros de la institución seleccionada mediante `establishmentId` |
| Configurar inicio/fin y preparar automáticamente borradores anuales | Sin permiso | Permitido; conserva respuestas existentes |
| Crear borrador y guardar valoración/evidencias | Sus propios registros durante el período abierto, excepto el área ya enviada | Sin permiso |
| Enviar un área completa y habilitar su PMI | Su propia área, dentro del período abierto | Sin permiso |
| Registrar observaciones de revisión | Pendiente de reglas de revisión | Funcionalidad pendiente |

Para Secretaría se exige el UUID de la institución: su propio UUID identifica un usuario de tipo `Secretary` y no corresponde a un `Establishment`. Una institución recibe `403` si usa el selector para consultar datos de otra. Las escrituras toman la institución de la sesión autenticada.

El registro anual conserva `DRAFT`. V12 agrega `SelfEvaluationAreaSubmission` para la entrega independiente de cada área, cuya respuesta es `DRAFT` o `SUBMITTED` con fecha. La consulta de Secretaría está disponible por API; su bandeja, observaciones y reapertura siguen pendientes. La entrega congela únicamente las valoraciones del área enviada y habilita su PMI. Detalles en [envío por área](envio-autoevaluacion-por-area.md).
