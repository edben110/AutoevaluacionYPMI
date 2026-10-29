# PMI: análisis y priorización de factores críticos

Primera etapa del PMI, solicitada por Juan el 21 de octubre de 2026. Se utiliza **solo la hoja `Anexo 2`** del archivo `24. IEM CIUDADELA EDUCATIVA PASTO AUTOEV 2025 Y PMI 2026-2028 G. ACADEMICA.xlsx`, ubicado en la carpeta de evidencias. No se importan los componentes antiguos ni los datos de la institución del ejemplo.

## Alcance y fuentes

El Anexo 2 se titula **«Analizar y priorizar los factores críticos»**. La fila 8 indica proyección a **un año**. Las filas 12–13 contienen proceso, componente, factores críticos, Urgencia, Tendencia, Impacto y total. Las fórmulas G14:G17 suman U + T + I. Ejemplos de comprobación: 5/4/5 → 14; 3/3/4 → 10; 4/3/5 → 12.

El diagrama de flujo local del equipo planteaba candidatos valorados en 1 o 2 y un componente adicional. Juan precisó y aprobó el 21 de octubre de 2026 la regla que se aplica ahora: **un PMI por área**, habilitado tras completar sus valoraciones y enviar explícitamente la autoevaluación del área; candidatos del menor estado presente (1, 2 o 3), sin candidatos si todos están en 4. El diagrama de clases representa esta etapa mediante `PMI`, `PrioritaryComponent` y `ComponentValuation`; la selección contextual amplía el diseño inicial.

**La regla vigente de candidatos y selección es la aclaración expresa de Juan, no una restricción del Excel.** Solo pasan a formulación los componentes empatados en el mayor U/T/I: uno si el máximo es único, ambos si son dos, o exactamente dos elegidos por la institución si hay tres o más. No se completa el cupo con puntajes inferiores. Si no hay 15, el máximo puede ser 14, 13, etc.

Esta pantalla cubre análisis y priorización. La formulación continúa en el [Anexo 3](pmi-anexo3.md); las observaciones de Secretaría y el seguimiento formal siguen pendientes.

## Reglas y criterios de aceptación

| Regla | Actor | Fuente | Validación y resultado |
| --- | --- | --- | --- |
| Identificar institución, año y área de origen | Institución; Secretaría consulta | Excel C6; diagrama y aclaración de Juan | La escritura toma el usuario de sesión. Un PMI por autoevaluación anual y área, sin mezclar selección ni recursos entre áreas. |
| Habilitar PMI al completar y enviar el área | Sistema | Aclaración aprobada por Juan | Todos los componentes activos con proceso y área activos deben tener una valoración guardada. Los textos y evidencias siguen siendo opcionales en esta iteración. Un área sin componentes activos no cuenta como completa. Además debe enviarse explícitamente; V12 registra el envío y congela las respuestas de esa área. |
| Mostrar candidatos de menor estado | Institución | Aclaración aprobada por Juan | Solo componentes del mínimo presente: Existencia; en su ausencia Pertinencia; en ausencia de ambas, Apropiación. Si todos están en Mejoramiento continuo no hay candidatos. Se usan los niveles del State en contexto de área, sin alterar su clasificación histórica `isImprovementChance()`. |
| Organizar por área, proceso y componente | Institución | Excel A11 y A12:B13 | Usa las relaciones actuales del catálogo, también para las áreas distintas de la académica del ejemplo. |
| Describir factores críticos por componente | Institución | Excel C12 | Texto obligatorio de hasta 10.000 caracteres, guardado en su valoración prioritaria. |
| Asignar U/T/I | Institución | Excel A10 y D13:F13; flujo del equipo | Cada valor es entero entre 1 y 5; no se calcula desde el estado de autoevaluación. |
| Calcular total | Sistema | Fórmulas G14:G17 del Excel | Suma los tres valores guardados, de 3 a 15; no acepta un total calculado por el usuario. |
| Completar U/T/I antes de formular | Institución | Interpretación de «al calificar el Anexo 2», aplicada al borrador | Todos los candidatos del área deben tener factores y U/T/I guardados antes de determinar el máximo definitivo. |
| Pasar hasta dos máximos al Anexo 3 | Sistema e institución | Aclaración aprobada por Juan | Un máximo único pasa solo; dos empatados pasan automáticamente; tres o más exigen escoger exactamente dos del grupo máximo. Nunca se admite un componente de menor puntaje para completar el cupo. |
| Resolver empates sin desempate arbitrario | Institución | Aclaración aprobada por Juan | Con más de dos máximos se conserva una selección previa de dos si ambos siguen en el máximo; en otro caso se pide elegir de nuevo. |
| Conservar trazabilidad ante cambios de autoevaluación | Sistema e institución | Relación `PrioritaryComponent` → `ComponentValuation`; decisión de implementación del borrador | No borra factores ni U/T/I. Si el estado de origen deja de ser candidato, el registro se muestra para consulta y la selección exige revisión. |
| Registrar fecha y proyección | Sistema | Excel filas 7–8 | Proyección de un año y fecha de diligenciamiento al crear el borrador, con fecha de Bogotá. El año de autoevaluación y la fecha de diligenciamiento son datos distintos. |

La selección se calcula sobre las puntuaciones guardadas. Mientras falte analizar un candidato, `prioritizationComplete` y `canFormulate` son falsos. La consulta deriva la selección automática de uno o dos máximos sin escribir registros.

## Uso de la pantalla

1. Entrar como institución y abrir **Planes de mejoramiento por área** en el espacio principal.
2. Elegir el año y área. Cada tarjeta muestra valorados/total y habilita su PMI únicamente cuando el área está completa y enviada; no exige completar las otras tres ni mantener abierto el período de autoevaluación.
3. Revisar los candidatos del menor estado, agrupados por proceso, escribir sus factores críticos y asignar U/T/I.
4. Guardar el análisis de cada componente. El total, los máximos y la selección se actualizan a partir de los valores persistidos.
5. Si hay tres o más máximos, elegir exactamente dos y guardar la selección. Si hay uno o dos, pasan automáticamente. Abrir el Anexo 3 cuando esté habilitado.

Los campos pendientes de otros componentes se conservan al guardar uno. Los totales y la selección se refieren siempre a lo guardado. Los nuevos estilos usan las variables de color SEM y la tipografía de Antonio; no sustituyen las hojas globales.

## API

| Método y ruta | Función | Permisos |
| --- | --- | --- |
| `GET /api/pmi/{year}` | Resumen `year, areas` con completitud, avance y candidatos por área; no crea registros | Institución propia; Secretaría con `?establishmentId=<UUID>` |
| `GET /api/pmi/{year}/areas/{areaId}` | Consulta la matriz del área y sus históricos; no crea registros | Institución propia; Secretaría con selector institucional |
| `PUT /api/pmi/{year}/areas/{areaId}/components/{componentId}` | Guarda factores críticos y U/T/I de un candidato del área | Institución propia |
| `PUT /api/pmi/{year}/areas/{areaId}/selection` | Guarda una selección compuesta únicamente por máximos, con la cantidad requerida | Institución propia |

Análisis de un componente:

```json
{
  "criticalFactors": "La institución describe aquí los factores críticos del componente.",
  "urgency": 5,
  "tendency": 4,
  "impact": 5
}
```

Selección para tres o más máximos (ambos UUID deben corresponder al máximo actual):

```json
{
  "componentIds": ["<UUID de un máximo>", "<UUID de otro máximo>"]
}
```

La respuesta contiene la institución, autoevaluación y año de origen, fecha de diligenciamiento, proyección, candidatos, factores, U/T/I, totales, máximos, selección y aviso de revisión. Incluye `selfEvaluationComplete`, `selfEvaluationSubmitted` y `submittedAt` por separado. Antes del primer guardado, el identificador y la fecha de diligenciamiento del PMI son nulos. No existen puntuaciones ni factores precargados desde el Excel.

V12 exige el [envío explícito de la autoevaluación por área](envio-autoevaluacion-por-area.md) antes de escribir en el PMI. No da por enviadas las autoevaluaciones anteriores. Las consultas e históricos se conservan.

## Persistencia, permisos y decisiones pendientes

V9 agrega `pmi` y `prioritary_component`, con relaciones a la autoevaluación y valoración originales. Conserva V1–V8 sin modificaciones. Las operaciones de escritura se serializan por autoevaluación para evitar borradores duplicados y selecciones concurrentes de más de dos componentes.

V11 añade el área y sustituye la unicidad anual por `(self_evaluation_id, area_id)`. Separa los planes previos según el área de sus componentes, conservando UUID de prioridades, objetivos y sus hijos, fechas, factores e importes. La actualización de valoraciones comparte el bloqueo del PMI. Las rutas antiguas de escritura sin área ya no se utilizan. Los análisis y objetivos que dejaron de ser elegibles permanecen para consulta; eliminar un objetivo histórico propio sigue siendo una acción explícita con confirmación.

Secretaría puede consultar la institución seleccionada, pero no editar sus factores ni U/T/I. Sus futuras observaciones no se reutilizan como texto institucional.

El PMI puede prepararse después de cerrar la autoevaluación. No se reutilizan sus fechas como un período PMI ni se inventa un calendario de entregas nuevo. El equipo debe confirmar el calendario propio y el cierre formal del PMI al desarrollar esas etapas. Tampoco se presupone que la fecha de diligenciamiento determine automáticamente el año objetivo de ejecución: aquí solo se registra la proyección de un año indicada en el Anexo 2.

Verificación de la regla por área: suite completa de **89 pruebas correctas** con PostgreSQL local, incluidas 21 de priorización, 14 de formulación y una migración V10→V11 con prioridades y objetivos de dos áreas. Compilación Vue/TypeScript y ESLint correctos. La comprobación de navegador con respuestas aisladas cubre cuatro tarjetas de área y progreso, bloqueo de incompleta, Apropiación, todos en Mejoramiento continuo, dos máximos automáticos con puntaje 14 y elección de dos entre tres empatados; escritorio y móvil de 390 px sin desbordamiento.
