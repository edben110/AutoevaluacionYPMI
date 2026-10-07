# Períodos anuales de autoevaluación

Regla confirmada por Juan el 6 de octubre de 2026: **Secretaría configura las fechas de inicio y fin** y el sistema crea automáticamente los borradores para todas las instituciones registradas. Este período anual abarca las cuatro áreas de gestión. Es una decisión de producto; no se atribuyen estas fechas ni la creación automática a la Guía 34.

## Funcionamiento

1. Secretaría entra en **Mi espacio → Administrar períodos**.
2. Indica el año de la autoevaluación, la fecha de inicio y la fecha de fin y pulsa **Habilitar período**.
3. La publicación crea un borrador vacío por institución y año. Si ya existe, conserva su identificador, sus respuestas y las fechas de diligenciamiento; solamente lo vincula al período. Volver a guardar no duplica borradores.
4. La institución encuentra las cuatro áreas únicamente cuando existe un período abierto por Secretaría. El espacio consulta los períodos publicados; si hay varios abiertos, permite elegir uno. No hay un año libre ni un botón institucional para crear el borrador. Regla de visibilidad aclarada por Juan el 6 de octubre de 2026.
5. Secretaría puede **Editar fechas** para ajustar o extender el período. Si está programado, cerrado, eliminado o sin configurar, las áreas y formularios no se muestran a la institución; tampoco se cargan al entrar por enlace directo. La disponibilidad se vuelve a consultar al regresar a la pestaña o actualizar el espacio.
6. Secretaría puede **Eliminar período** y confirmar la acción. Se retiran sus fechas de habilitación y se cierra el diligenciamiento de ese año; los borradores, respuestas, evidencias y sus marcas de tiempo se conservan. Volver a configurar el mismo año vincula los borradores existentes sin duplicarlos. Esta opción fue solicitada por Juan el 6 de octubre de 2026.

Ambos días límite están incluidos completos, con fecha del servidor en **America/Bogota**. El cierre se calcula en cada petición; no requiere tareas programadas ni reiniciar el servidor. Un período de un solo día es válido. La fecha de fin nunca puede ser anterior al inicio.

El año identifica la autoevaluación; las fechas pueden pertenecer a otro año calendario. No se impone una restricción que impida, por ejemplo, diligenciar una evaluación del año anterior.

Los registros anteriores a V8 se conservan. V8 no publica fechas arbitrarias. Una institución registrada después de publicar necesita que Secretaría vuelva a guardar el período para preparar su borrador; el formulario institucional no ofrece crearlo manualmente. La consulta por API de las respuestas conservadas mantiene sus permisos existentes, aunque el formulario institucional solo se muestra durante un período abierto.

## API

| Método y ruta | Institución | Secretaría |
| --- | --- | --- |
| `GET /api/self-evaluation-periods` | Consultar períodos | Consultar períodos |
| `GET /api/self-evaluation-periods/{year}` | Consultar fechas/estado | Consultar fechas/estado |
| `PUT /api/self-evaluation-periods/{year}` | Sin permiso | Publicar o ajustar fechas y preparar borradores |
| `DELETE /api/self-evaluation-periods/{year}` | Sin permiso | Eliminar la habilitación, conservando autoevaluaciones; devuelve `204` |
| `PUT /api/self-evaluations/{year}` | Su propio borrador, dentro del período | Sin permiso |
| `PUT /api/self-evaluations/{year}/valuations/{componentId}` | Sus propias respuestas, dentro del período | Sin permiso |

Ejemplo de cuerpo de configuración (escoger las fechas reales antes de usarlo):

```json
{
  "startDate": "2026-10-06",
  "finishDate": "2026-10-31"
}
```

El DTO devuelve `year`, `startDate`, `finishDate`, `status` (`SCHEDULED`, `OPEN`, `CLOSED`), `writable`, `today`, `timeZone`, `createdAt` y `updatedAt`. La ausencia de configuración responde `404`. Fechas inválidas responden `400`. Escribir sin período abierto responde `403` con un mensaje sobre el período. Las consultas de respuestas conservan los permisos de pertenencia institucional existentes.

## Modelo y persistencia

- `PeriodRecord` corresponde al valor temporal del diagrama del equipo: inicio, fin, `include()` y `daysDuration()`; la duración cuenta ambos días.
- `SelfEvaluationPeriod` publica un `PeriodRecord` por año, con usuario de Secretaría creador y marcas de tiempo.
- `SelfEvaluation` se vincula al período del mismo año. La relación puede estar vacía en registros históricos, conservados por la migración.
- V8 crea `self_evaluation_period` y `self_evaluation.period_year`, con integridad de fechas, año, creador y relación. No modifica las migraciones aplicadas.
- Las escrituras institucionales mantienen un bloqueo de lectura del período durante su transacción; la edición de fechas bloquea su fila para evitar que cambien mientras se guarda una respuesta.

La publicación habilita el diligenciamiento y prepara borradores. El envío formal, la revisión y las observaciones de Secretaría siguen pendientes; Secretaría no edita respuestas institucionales.

La eliminación desvincula los borradores y elimina el período en una sola transacción, protegida por el bloqueo de su fila. No requiere cambios de esquema ni una nueva migración: V8 ya permite conservar autoevaluaciones sin período. Eliminar un período inexistente responde `404`.

## Verificación

`SelfEvaluationPeriodTests` verifica permisos, generación de borradores, conservación al reprogramar, ausencia de período, límites incluidos, cierre, fechas inválidas y cambio de día en Bogotá. También prueba eliminación con respuestas guardadas, bloqueo de escritura posterior, reapertura sin duplicados, permisos de eliminación y conservación de otros años. Las pruebas son transaccionales: no habilitan ni eliminan un período de negocio ni conservan instituciones de prueba en la base local.
