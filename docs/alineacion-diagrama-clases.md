# Alineación con los diagramas del equipo

Revisión del 6 de octubre de 2026. Fuentes: [diagrama de clases](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-Diagrama-de-clases) y [diagrama de flujo](https://github.com/edben110/AutoevaluacionYPMI/wiki/Link-diagrama-de-flujo). Se leyeron las etiquetas de los SVG originales enlazados en la wiki; las copias locales están en `.codex/references/`, ignoradas por Git.

## Modelo actual y trabajo pendiente

| Clase del diagrama | Código actual | Correspondencia y pendientes |
| --- | --- | --- |
| `User`, `Establishment`, `Secretary`, `EmailRecord` | Mismas clases existentes de `anton` | Se conserva el modelo de usuarios. Los permisos y consultas se implementan en controlador/servicios, manteniendo las entidades para persistencia. |
| `IntegralGestion`, `Area`, `Process`, `Component` | `IntegralManagement`, `Area`, `Process`, `Component` | Se conserva la base y las relaciones área → proceso → componente. El catálogo V6 agrega el inventario oficial. |
| `SelfAssessment` | `SelfEvaluation` | Una institución, año, valoraciones y relación al período anual que contiene `PeriodRecord`. El año se guarda como entero; la colección/snapshot de áreas del diagrama sigue pendiente. |
| `ComponentValuation` | `ComponentValuation` | Referencia al componente, autoevaluación, estado y enlace de evidencia. `proofLink` corresponde a `evidenceUrl`. `evidenceNote` es la nota de ubicación de evidencia de la institución; no sustituye las observaciones de Secretaría del diagrama. |
| `ComponentValue` | `ComponentValue` | Enum con los cuatro estados; se conserva la codificación 1–4 de la BD/API y nombres con ortografía normalizada. |
| Interfaz `ValueState` y cuatro estados | `ValuationState` y cuatro estados | `getValue()`, `isImprovementChance()` e `isStrength()` implementados. Conteo por estado delegado al objeto State. El nombre de la interfaz difiere; su función corresponde al diagrama. |
| `PrioritaryComponent` | Pendiente | Debe referenciar `ComponentValuation`, almacenar factores críticos, U/T/I y observaciones. Todavía no se reemplaza por un simple subtotal ni se crean factores críticos automáticamente. |
| `PMI`, `Objective`, `Goal`, `Activity`, `Indicator` | Pendientes | Se tomarán sus relaciones y campos del diagrama al implementar formulación y seguimiento. |
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
| Crear borrador y guardar valoración/evidencias | Sus propios registros durante el período abierto | Sin permiso |
| Registrar observaciones de revisión | Pendiente de reglas de revisión | Funcionalidad pendiente |

Para Secretaría se exige el UUID de la institución: su propio UUID identifica un usuario de tipo `Secretary` y no corresponde a un `Establishment`. Una institución recibe `403` si usa el selector para consultar datos de otra. Las escrituras toman la institución de la sesión autenticada.

El borrador actual solo dispone del estado `DRAFT`. La consulta de Secretaría ya está disponible por API; todavía están pendientes su pantalla, el envío formal a revisión, observaciones y devolución a la institución. Las diferencias anteriores quedan explícitas para seguir el diagrama al ampliar el modelo.
