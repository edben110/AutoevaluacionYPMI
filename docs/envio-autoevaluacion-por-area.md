# Envío por área y guardado automático

Solicitud de Juan del 21 de octubre de 2026: agregar **Enviar autoevaluación de esta área** y guardar automáticamente las respuestas. Complementa la [valoración de componentes](valoracion-componentes.md) y la [priorización del PMI](pmi-anexo2.md). Cada una de las cuatro áreas conserva su envío y habilita exclusivamente su propio PMI.

## Reglas y trazabilidad

| Regla | Actor | Fuente | Validación y resultado |
| --- | --- | --- | --- |
| Guardado automático por componente | Institución | Solicitud de Juan; decisión técnica de interfaz | Tras unos 800 ms sin editar, envía la última versión válida al endpoint de valoración existente. Muestra cambios pendientes, guardando, guardado o error; permite guardar ahora/reintentar. |
| Seguimiento anterior antes de la autoevaluación | Institución | Confirmación de Juan del 26 de octubre | La creación de borrador, escritura y envío del año nuevo exigen finalizar los seguimientos de todos los PMI del año anterior. La consulta de requisitos informa áreas pendientes; sin PMI previo no impone un seguimiento inexistente. |
| Completar no equivale a enviar | Institución | Aclaración de Juan sobre el botón de envío por área | Todos los componentes activos del área deben tener nivel guardado, pero el PMI permanece bloqueado hasta el envío explícito. |
| Enviar solo el área elegida | Institución | Solicitud expresa de un PMI por área | Verifica período abierto y valoraciones completas; confirma en pantalla el alcance del envío. Las otras áreas siguen siendo borradores editables. |
| No enviar datos pendientes | Institución y sistema | Integridad necesaria del guardado automático | Antes del POST de envío, espera las peticiones en curso y guarda las últimas ediciones del área. Si falla un guardado o hay datos inválidos, conserva los cambios y no envía. |
| Conservar la respuesta enviada | Institución; Secretaría consulta | Decisión de implementación del envío; contexto del equipo sobre observaciones para la siguiente entrega | El área enviada queda para consulta. La API rechaza modificaciones posteriores de sus valoraciones. No se implementa una reapertura implícita ni una aprobación de Secretaría. |
| Reintentar envío sin duplicarlo | Sistema | Integridad de la operación | Conserva el mismo instante y un único registro incluso si el reintento ocurre después de cerrar o retirar el período. |
| Conservar permisos institucionales | Institución y Secretaría | Separación de actores del flujo del equipo | Secretaría puede consultar con `establishmentId`; solo la institución autenticada envía su propia área. |

Los textos y evidencias siguen siendo opcionales en esta versión: «completa» significa que todos los componentes activos, con padres activos, tienen una valoración. No se cambia la aptitud de fortalezas/oportunidades por estado ni la selección por menor estado y máximo U/T/I.

Por aclaración de Juan del 26 de octubre, eliminar y volver a publicar el mismo año reinicia las cuatro áreas: se retiran valoraciones y envíos anteriores y el PMI vuelve a exigir completitud y envío. El reintento idempotente descrito arriba aplica antes de esa nueva publicación. Editar fechas del período existente conserva los envíos. Véanse [períodos](periodos-autoevaluacion.md).

## Persistencia y API

V12 agrega `self_evaluation_area_submission`: UUID, autoevaluación, área y `submitted_at`, con unicidad por autoevaluación+área. No convierte en enviados los borradores anteriores, aunque tengan todos los componentes valorados. El registro anual conserva su estado `DRAFT`; el estado de cada área se representa por separado mediante `SelfEvaluationAreaSubmission`. La ausencia de registro corresponde a `DRAFT` y su presencia a `SUBMITTED`.

| Método y ruta | Función y permiso |
| --- | --- |
| `GET /api/self-evaluations/{year}/areas/{areaId}` | Estado propio; Secretaría con selector institucional. Devuelve `year`, `areaId`, `areaName`, `status`, `submittedAt`, `totalComponents`, `valuedComponents`, `complete` y `canSubmit`. |
| `POST /api/self-evaluations/{year}/areas/{areaId}/submit` | Sin cuerpo. Solo institución propia; devuelve el mismo contrato con `SUBMITTED`. Primera entrega exige período abierto y área completa. |
| `PUT /api/self-evaluations/{year}/valuations/{componentId}` | Endpoint existente usado por el guardado automático. No permite modificar componentes del área ya enviada. |

Los resúmenes y respuestas del PMI incorporan `selfEvaluationSubmitted` y `submittedAt`, manteniendo `selfEvaluationComplete` como conteo independiente. Su escritura y formulación requieren área completa **y enviada**. Las consultas y la eliminación explícita de objetivos históricos propios conservan sus permisos previos.

Guardar valoraciones y enviar comparten el bloqueo de autoevaluación; ambos toman primero el bloqueo de período. Así, un guardado que llegue después del envío no modifica las respuestas entregadas. Las fechas se conservan con precisión de PostgreSQL para que el primer resultado y los reintentos coincidan.

## Comportamiento del guardado automático

- Cada componente conserva su última versión confirmada y una única petición en curso. Si se escribe durante esa petición, la edición permanece visible y se guarda después; una respuesta anterior no la marca falsamente como guardada.
- No guarda al cargar datos ni envía combinaciones inválidas de estado/textos o campos que superen el límite. La referencia de evidencia admite enlaces o texto libre desde V13. No elimina textos incompatibles automáticamente: permite corregirlos o quitarlos explícitamente.
- Los errores muestran la respuesta pendiente y permiten reintentar. «Guardado» significa confirmado por el servidor; no se promete almacenamiento sin conexión.
- Antes de enviar se vacían los guardados pendientes de toda el área. Un fallo impide el POST de envío y conserva el borrador.
- Cambiar de área/año o salir con datos pendientes solicita confirmación; los temporizadores se cancelan y las respuestas tardías no se muestran en otro contexto.

Para probar: reiniciar el backend con V12, abrir un área dentro del período habilitado, valorar sus componentes y esperar los indicadores de guardado. Pulsar **Enviar autoevaluación de esta área**, confirmar y abrir el PMI desde el enlace de esa área. Las tarjetas de las otras áreas permanecen pendientes de sus propios envíos.

## Límites de esta iteración

La bandeja, las observaciones por componente y el historial se incorporaron el 27 de octubre: véase [revisión e historial](revision-secretaria-historial.md). No incluye notificaciones ni reapertura de un área enviada. El catálogo todavía es mutable: agregar componentes activos después del envío puede alterar su completitud; no reabre automáticamente las respuestas entregadas. El versionado del catálogo y cualquier reapertura deben definirse con el equipo.

Las pruebas `SelfEvaluationAreaSubmissionTests` cubren área completa sin enviar, rechazo de envíos incompletos, persistencia, idempotencia, permisos, bloqueo de respuestas, independencia entre áreas y cambios de período. Las pruebas del PMI se adaptan para partir de áreas enviadas; las alteraciones de origen usadas para comprobar históricos se simulan dentro de las transacciones de prueba, no mediante una reapertura pública.

Verificación inicial del 21 de octubre: **96 pruebas de backend correctas**, incluidas siete de envío por área; compilación Vue/TypeScript y ESLint correctos. En navegador con respuestas aisladas se comprobaron la pausa de guardado, ausencia de escrituras al cargar, ediciones y reversión durante peticiones lentas, desconexión y reintento conservando textos, envío después de guardar las últimas versiones de todas las filas, persistencia al recargar, consulta de área enviada e independencia de las otras áreas. V13 reemplaza la restricción de URL por evidencia de texto libre; su verificación se documenta en [ayuda de componentes y evidencias](ayuda-componentes-evidencias.md). La vista móvil de 390 px no presenta desbordamiento. Las comprobaciones visuales no envían autoevaluaciones reales.
