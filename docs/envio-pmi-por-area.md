# Finalización y envío del PMI por área

Decisión expresa de Juan del 28 de octubre de 2026: Secretaría debe consultar el PMI únicamente después de que la institución pulse **Finalizar y enviar PMI del área**. Reemplaza la consulta de avances incompletos descrita en la revisión inicial. Se conserva un PMI por autoevaluación anual y área; la autoevaluación mantiene su envío independiente. Esta regla es una decisión del proyecto y no se atribuye como requisito textual de la Guía 34.

## Comportamiento

| Regla | Actor y datos | Validación |
| --- | --- | --- |
| Borrador privado | Institución propietaria; anexos 2–4 | Guardar no entrega el PMI. Secretaría no recibe su UUID ni cambios de borrador en el paquete y obtiene 404 si intenta consultar directamente cualquiera de sus anexos. El resumen de PMI de Secretaría incluye solo áreas con PMI enviado. |
| Revisar antes de enviar | Institución; panel en Anexo 5 | Consulta los pendientes de los datos guardados. El botón requiere autoevaluación completa y enviada, período publicado, todos los candidatos priorizados y selección válida. |
| Completar Anexo 3 | Institución; componentes seleccionados | Cada componente seleccionado necesita al menos un objetivo con descripción, una meta con descripción y una fila. Cada fila requiere indicador, actividades, los tres importes (cero es válido), responsable, inicio/fin e instrumentos/frecuencia de recolección. Se conservan las validaciones de fechas e importes existentes; no se agrega una regla de suma entre fuentes de recursos. |
| Completar Anexo 4 | Institución; fichas de indicadores de objetivos vigentes | Tipo, objetivo del indicador, unidad, variables, fórmula, fuente, metodología, periodicidad y responsable completos. Las fórmulas continúan siendo texto, sin cálculo automático. |
| Enviar explícitamente | Institución autenticada; PMI del área | Confirmación en pantalla, validación de servidor bajo el mismo bloqueo anual que las ediciones, fecha persistida e idempotencia: repetir no cambia la fecha ni duplica entregas. |
| Consulta después del envío | Institución y Secretaría | La institución conserva consulta de anexos 2–4; no puede cambiar puntajes/selección, crear/editar/eliminar objetivos ni modificar fichas. Secretaría no edita ni comenta el PMI. La publicación cambia la versión del paquete recibido y lo vuelve pendiente si ya se había revisado. |
| Seguimiento posterior | Institución; Anexo 5 | No se requieren estado, porcentaje ni observaciones para enviar el PMI. Su diligenciamiento sigue habilitándose al abrir el período siguiente y su finalización sigue siendo requisito de la próxima autoevaluación. Enviar el PMI no bloquea ese seguimiento. |

Los objetivos históricos se conservan para consulta según las reglas existentes. La validación de completitud se aplica a los objetivos de los componentes seleccionados; no crea ni elimina datos históricos. No se agrega una reapertura de PMI ni una aprobación de Secretaría.

## Persistencia y API

V19 agrega `pmi.submitted_at`, nullable: los registros previos permanecen como borradores hasta su envío explícito. No hay publicación automática por migración. La eliminación del período sigue eliminando sus PMI y el estado de envío con ellos.

- `GET /api/pmi/{year}/areas/{areaId}/submission`: solo institución propia; devuelve estado, fecha, `canSubmit` y pendientes.
- `POST /api/pmi/{year}/areas/{areaId}/submission`: solo institución propia; finaliza/envía o devuelve el envío existente.
- Anexo 2 devuelve `status: DRAFT | SUBMITTED`. Anexos 3 y 4 agregan `pmiSubmittedAt` y deshabilitan sus permisos de edición después del envío.
- La bandeja y el historial muestran **PMI enviado** o **PMI aún no enviado**. El paquete puede aparecer antes por la autoevaluación enviada, sin revelar el borrador de PMI.

El panel de Anexo 5 informa pendientes, requiere confirmación y permite reintentar errores de envío. La tabla y sus espacios de seguimiento conservan los bloqueos de período, la paleta SEM y el desplazamiento horizontal.

## Verificación

Suite completa de Java: **168 pruebas correctas**, con ocho casos de envío que cubren borradores privados en todos los anexos/API/bandeja, completitud, envío explícito persistente e idempotente, bloqueo de las seis operaciones de edición, seguimiento del año siguiente, áreas independientes y autorización. La migración V19 y la validación de Hibernate se ejecutaron con PostgreSQL local. Se actualizaron las expectativas anteriores que permitían a Secretaría leer borradores.

Compilación Vue/TypeScript y ESLint correctos. Chrome con datos aislados comprobó pendientes que bloquean el botón, error de envío recuperable, confirmación explícita, estado enviado tras recargar, consulta de anexos 2–4 sin edición, Anexo 5 sin respuestas anticipadas, recepción por Secretaría y vista de 390 px sin desbordamiento de página. Las pruebas no enviaron los cuatro PMI locales de demostración.
