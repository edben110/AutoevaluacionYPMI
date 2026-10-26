# Períodos anuales de autoevaluación

Regla confirmada por Juan el 6 de octubre de 2026: **Secretaría configura las fechas de inicio y fin** y el sistema crea automáticamente los borradores para todas las instituciones registradas. Este período anual abarca las cuatro áreas de gestión. Es una decisión de producto; no se atribuyen estas fechas ni la creación automática a la Guía 34.

## Funcionamiento

1. Secretaría entra en **Mi espacio → Administrar períodos**.
2. Indica el año de la autoevaluación, la fecha de inicio y la fecha de fin y pulsa **Habilitar período**.
3. La publicación crea un borrador vacío por institución y año. Si ya existe, conserva su identificador, sus respuestas y las fechas de diligenciamiento; solamente lo vincula al período. Volver a guardar no duplica borradores.
4. La institución encuentra las cuatro áreas únicamente cuando existe un período abierto por Secretaría. El espacio consulta los períodos publicados; si hay varios abiertos, permite elegir uno. No hay un año libre ni un botón institucional para crear el borrador. Regla de visibilidad aclarada por Juan el 6 de octubre de 2026.
5. Secretaría puede **Editar fechas** para ajustar o extender el período. Si está programado, cerrado, eliminado o sin configurar, las áreas y formularios no se muestran a la institución; tampoco se cargan al entrar por enlace directo. La disponibilidad se vuelve a consultar al regresar a la pestaña o actualizar el espacio.
6. Secretaría puede **Eliminar período** y confirmar la acción. Se retiran sus fechas de habilitación, se cierra el diligenciamiento y se eliminan los PMI de todas las instituciones y áreas de ese año: priorización, objetivos, metas, actividades, indicadores con sus fichas y todos los controles de seguimiento (anexos 2–5). Las autoevaluaciones, respuestas, evidencias, envíos y marcas de tiempo se conservan. Volver a publicar el mismo año vincula las autoevaluaciones existentes y permite iniciar PMI nuevos; no recupera los PMI borrados. Esta ampliación fue solicitada expresamente por Juan el 24 de octubre de 2026 y reemplaza la conservación anterior del PMI al retirar la habilitación.

Ambos días límite están incluidos completos, con fecha del servidor en **America/Bogota**. El cierre se calcula en cada petición; no requiere tareas programadas ni reiniciar el servidor. Un período de un solo día es válido. La fecha de fin nunca puede ser anterior al inicio.

El año identifica la autoevaluación; las fechas pueden pertenecer a otro año calendario. No se impone una restricción que impida, por ejemplo, diligenciar una evaluación del año anterior.

Los registros anteriores a V8 se conservan. V8 no publica fechas arbitrarias. Una institución registrada después de publicar necesita que Secretaría vuelva a guardar el período para preparar su borrador; el formulario institucional no ofrece crearlo manualmente. La consulta por API de las respuestas conservadas mantiene sus permisos existentes, aunque el formulario institucional solo se muestra durante un período abierto.

## API

| Método y ruta | Institución | Secretaría |
| --- | --- | --- |
| `GET /api/self-evaluation-periods` | Consultar períodos | Consultar períodos |
| `GET /api/self-evaluation-periods/{year}` | Consultar fechas/estado | Consultar fechas/estado |
| `PUT /api/self-evaluation-periods/{year}` | Sin permiso | Publicar o ajustar fechas y preparar borradores |
| `DELETE /api/self-evaluation-periods/{year}` | Sin permiso | Eliminar habilitación y todos los PMI/anexos de ese año, conservando autoevaluaciones; devuelve `204` |
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

La eliminación bloquea el período y sus autoevaluaciones (ordenadas por UUID) antes de eliminar los PMI del año y sus anexos mediante `PMIPeriodCleanupService`. Utiliza los mismos bloqueos de autoevaluación que las escrituras del PMI. Elimina primero los seguimientos, después objetivos y sus hijos mediante las cascadas existentes, prioridades y planes; finalmente desvincula las autoevaluaciones y retira el período. Todo ocurre en una transacción. No modifica datos de otros años ni requiere migración nueva: se usan las relaciones existentes. Eliminar un período inexistente responde `404` sin limpiar datos.

La ausencia de período también bloquea nuevas escrituras de priorización para impedir recrear un PMI inmediatamente después de borrarlo. `periodConfigured` en el resumen y detalle del PMI identifica si existe el período; el panel institucional oculta los PMI de años retirados y los enlaces directos muestran la indisponibilidad. Cerrar por fecha o editar las fechas **no elimina** los PMI y conserva el acceso acordado después del cierre. Las respuestas de autoevaluación enviadas permanecen enviadas al volver a publicar el año.

## Verificación

`SelfEvaluationPeriodTests` verifica permisos, generación de borradores, conservación al reprogramar, ausencia de período, límites incluidos, cierre, fechas inválidas y cambio de día en Bogotá. También prueba eliminación con respuestas guardadas, bloqueo de escritura posterior, reapertura sin duplicados, permisos de eliminación y conservación de otros años. Las pruebas son transaccionales: no habilitan ni eliminan un período de negocio ni conservan instituciones de prueba en la base local.

`PMIPeriodDeletionTests` añade el caso completo de dos instituciones con PMI en las cuatro áreas y un año distinto conservado: comprueba eliminación de todos los anexos, conservación de respuestas/envíos, bloqueo de recreación sin período y reinicio vacío al publicarlo otra vez. Verifica además que editar/cerrar fechas conserva los PMI y que las eliminaciones no autorizadas o de períodos inexistentes no afectan los registros.

Verificación del 24 de octubre: **129 pruebas de backend correctas**, compilación Vue/TypeScript y ESLint correctos. El recorrido de navegador con datos aislados comprobó el aviso de eliminación de anexos 2–5, cancelación sin petición de borrado, confirmación sobre el año elegido, conservación visual de otro año cerrado, ocultamiento del PMI retirado en el panel y ausencia de formularios en enlaces directos sin período. La confirmación se revisó a 390 px sin desbordamiento horizontal. No se eliminaron períodos de negocio durante las pruebas.
