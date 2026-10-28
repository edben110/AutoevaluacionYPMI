# PMI: seguimiento del Anexo 5

Implementación del 24 de octubre de 2026 en `JuanM`. Fuente: hoja **Anexo 5 -2026**, «Revisión del cumplimiento del Plan de Mejoramiento Institucional», del Excel de evidencias `24. IEM CIUDADELA EDUCATIVA PASTO AUTOEV 2025 Y PMI 2026-2028 G. ACADEMICA.xlsx`. La hoja **Anexo 5 -2025** se consultó como ejemplo diligenciado, sin importar sus respuestas o evidencias institucionales.

## Corrección del ciclo confirmada por Juan el 26 de octubre

Esta aclaración reemplaza la habilitación inicial del seguimiento durante la propia formulación del PMI:

| Momento | Anexo 5 | Autoevaluación siguiente |
| --- | --- | --- |
| Se formula el PMI de 2026; no ha abierto el período 2027 | Consolidado de consulta, sin campos para responder | No habilitada por ese período |
| Secretaría abre el período 2027 | Permite registrar el seguimiento de 2026 | Bloqueada hasta finalizar los seguimientos pendientes de todos los PMI 2026 de la institución |
| Todas las filas de un área tienen estado, porcentaje y observaciones guardados y se pulsa **Finalizar seguimiento del área** | El área queda para consulta | Se habilita cuando todas las áreas con PMI anterior están finalizadas |

La habilitación se decide por las fechas del período publicado del año siguiente, con hora de Bogotá, no por el año del reloj del navegador. Para probar el ciclo antes de enero de 2027 se puede publicar el año de autoevaluación 2027 con fechas vigentes, comportamiento ya permitido por el modelo del período.

Los tres campos son obligatorios al finalizar, por confirmación expresa de Juan. Se permiten borradores parciales al guardar. No se exige ejecución del 100 % ni estado FIN: una actividad no iniciada o cancelada puede informarse con 0 % y sus observaciones. Todas las filas actuales deben estar completas en un mismo control, cuya fecha debe pertenecer al período abierto del año siguiente. Un PMI sin filas formuladas requiere completar primero su Anexo 3; no se marca finalizado automáticamente. Si no existe PMI del año anterior, no hay seguimiento inexistente que bloquee el primer ciclo.

V16 agrega `PMIFollowUpCompletion`: PMI, año de revisión, fecha del control, fecha de finalización y huella del contexto del plan. No considera finalizados los controles guardados por versiones anteriores. El botón de finalización verifica pertenencia, apertura del período y las tres respuestas en cada fila. El reintento conserva el mismo registro y fecha, incluso después de cerrar el período. Los datos guardados de un seguimiento finalizado quedan de consulta.

Si se modifica el contenido del plan o su conjunto de filas después de finalizar, la huella deja de corresponder: vuelve a ser necesario un control completo del plan actualizado. Los controles históricos conservan su contexto. Esto evita que una finalización antigua habilite la autoevaluación de un plan que luego cambió o perdió filas. El borrado completo del PMI al eliminar su período también elimina su finalización.

El consolidado se agrupa por proceso → componente → objetivo → meta. Cada indicador aparece una sola vez con sus actividades y plazos, sin repetirlo en una tabla de resumen. El estado global del seguimiento y los conteos se muestran por separado.

## Reglas y trazabilidad

| Regla / dato | Actor | Fuente | Validación y criterio de aceptación |
| --- | --- | --- | --- |
| Proceso, componente, objetivo, meta, indicador, actividades y plazos | Institución | Hoja 2026, columnas A–H y filas 14–22; continuidad con Anexo 3 | Se consultan de cada fila guardada del Anexo 3 del mismo PMI/área/año. No se vuelven a digitar ni se separan automáticamente bloques de actividades. Se incluyen filas con indicador o actividades; las completamente vacías no generan formularios. |
| Estado de ejecución | Institución | Convenciones A8 y columnas I–M | Selección manual y única: `NI` (No iniciada), `ESP` (En espera), `CANC` (Cancelada), `FIN` (Finalizada), `EJ` (En ejecución). No se deriva de fechas ni del porcentaje. |
| Porcentaje de ejecución | Institución | Columna N; hoja 2025 con celdas almacenadas como fracción y formato porcentual | La app usa 0–100, hasta dos decimales: escribir 40 significa 40 %. No se importa el 0,4 del Excel como 40 sin conversión; esta iteración no importa archivos. Pendiente y cero son distintos. |
| Observaciones y evidencia | Institución | Columna O de ambas hojas | Texto de avances, dificultades y ubicación/enlaces de evidencia; hasta 10.000 caracteres. No exige digitalizar documentos ni cargar archivos. Son observaciones institucionales, separadas de una futura revisión de Secretaría. |
| Fecha de control e historial | Institución | Fecha de control del Excel; seguimiento periódico en Guía 34 de 2026, pp. 99–104; aclaración de Juan del 26 de octubre | Una respuesta por fila y fecha. La escritura exige el período abierto del año siguiente y una fecha de control dentro de sus límites; los plazos del plan se conservan como contexto y pueden pertenecer al año anterior. |
| Contexto de los controles guardados | Institución / Secretaría | Necesidad de trazabilidad ante ajustes del plan; guía actualizada, pp. 102–105 | La primera escritura de cada fila/fecha conserva una instantánea de sus datos de origen. Renombrar o ajustar el plan no reescribe controles anteriores. Al actualizar un control se corrigen avance y observaciones conservando su contexto; una fecha nueva toma el plan vigente. |
| Permisos e históricos | Institución / Secretaría | Separación de actores y consolidado de lo planeado confirmado por Juan | Solo escribe la institución propietaria, durante la etapa de seguimiento y sobre filas todavía presentes del plan. El consolidado abarca lo formulado, incluso si una selección cambió. Secretaría consulta con selector institucional; no edita. Las filas retiradas conservan controles históricos para consulta. |
| Borradores parciales | Institución | Confirmación de Juan del 26 de octubre | Guardar permite pendientes; finalizar exige estado, porcentaje y observaciones en todas las filas. No se impone relación automática entre estado y porcentaje, ni completar Anexo 4 como condición adicional. El porcentaje mide ejecución de actividades, no sustituye la medición definida en la ficha del indicador. |

Fuente oficial complementaria: [Guía 34 actualizada de 2026](https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf), etapa de seguimiento (pp. 97–114), encuentros y evidencias (pp. 99–104), ajustes del plan (pp. 102–105). La referencia del Excel a pp. 74–76 corresponde a una edición anterior. Se implementa el formato municipal; no toda la metodología cualitativa de seguimiento de la guía queda cubierta por este anexo.

El resumen cuenta los estados de las respuestas guardadas. No promedia porcentajes de actividades diferentes ni inventa un porcentaje global del PMI. La coherencia pedagógica entre estado, porcentaje y evidencia se deja a la institución; las reglas municipales de cierre, validación y revisión formal siguen pendientes.

## Persistencia y diagrama

V15 agrega `pmi_follow_up`, vinculado al PMI del área, con unicidad `(pmi_id, control_date, source_row_id)`. `source_row_id` conserva la identidad de la fila de `Activity`; los datos contextuales quedan como `FollowUpSnapshot` embebido. No tiene borrado en cascada desde los hijos del Anexo 3: eliminar el origen no elimina sus seguimientos. El servidor verifica pertenencia mediante la formulación del PMI antes de toda escritura, y comparte el bloqueo de autoevaluación usado por Anexos 2–4.

`PMIFollowUp` amplía el diseño del equipo para registrar cumplimiento de las actividades. Se mantienen `PMI → Objective → Goal → Activity / Indicator`; la valoración del componente y su State no se modifican. El registro del seguimiento no es una observación de Secretaría ni cambia la respuesta de autoevaluación o la definición del indicador.

**Excepción por eliminación del período:** Juan confirmó el 24 de octubre que retirar el período desde Secretaría elimina todos sus PMI y anexos, incluidos estos controles históricos. La conservación descrita aquí aplica a cambios o eliminación de filas/metas/objetivos dentro de un PMI. La confirmación de Secretaría informa la eliminación completa. Véase [períodos de autoevaluación](periodos-autoevaluacion.md).

Al consultar una fecha se combinan sus respuestas guardadas con las filas vigentes del plan que siguen pendientes de registrar. Las respuestas guardadas muestran su contexto original; las filas aún sin respuesta muestran el plan actual. Por tanto, no se presenta la consulta como una versión congelada de todo el PMI en esa fecha. Las respuestas guardadas cuyo origen desaparece se añaden para consulta y se identifican como retiradas.

## API

| Método / ruta | Actor | Resultado |
| --- | --- | --- |
| `GET /api/pmi/{year}/areas/{areaId}/follow-ups?controlDate=AAAA-MM-DD` | Institución propia; Secretaría con `establishmentId` | Cabecera del área, fecha, `availableDates`, permiso contextual `canEdit` y filas actuales/guardadas. Sin fecha usa el día de America/Bogota. GET no crea registros. |
| `PUT /api/pmi/{year}/areas/{areaId}/follow-ups/{controlDate}/rows/{rowId}` | Institución propia | Guarda estado, porcentaje y observaciones; devuelve solo esa fila y su contexto. La fecha y el UUID de fila forman la identidad del control dentro del PMI. |
| `POST /api/pmi/{year}/areas/{areaId}/follow-ups/{controlDate}/complete` | Institución propia | Finaliza el seguimiento del área tras verificar todas sus filas; devuelve el anexo para consulta. |
| `GET /api/self-evaluations/{year}/prerequisites` | Institución propia; Secretaría con `establishmentId` | Devuelve `ready`, `previousYear` y los PMI previos con su estado de finalización y enlace por área/año. |

El anexo agrega `reviewYear`, `phase` (`PLANNING`, `FOLLOW_UP`, `CLOSED`, `COMPLETED`), `completedAt`, `requiredRows`, `completedRows` y `canComplete`. `canEdit` depende de la etapa y la fecha de control. El panel y los enlaces directos de autoevaluación consultan los requisitos antes de cargar el formulario. El servidor verifica también el requisito en creación de borrador, guardado de valoraciones y envío; ocultar los botones no es la única protección. La comprobación y la finalización se coordinan con el bloqueo de la autoevaluación anterior, antes de bloquear la nueva.

Ejemplo:

```json
{
  "executionStatus": "EJ",
  "executionPercentage": "40.25",
  "observations": "Se realizaron las sesiones previstas para este corte. Actas físicas en rectoría y enlace al repositorio institucional."
}
```

El porcentaje se devuelve como cadena decimal con dos posiciones, o `null` si está pendiente. `PUT` sustituye los tres campos de esa fila/fecha; omitirlos los deja en `null`, manteniendo la identidad y contexto del control. Fechas o UUID mal formados, tipos desconocidos, porcentajes fuera de 0–100 o con más de dos decimales y textos demasiado largos reciben `400`. Recursos ajenos o de otra área/año no son editables.

## Interfaz y prueba manual

Desde Anexo 4, abrir **Registrar o consultar avances →**. También hay acceso desde Anexo 3. Ruta: `/pmi/:year/areas/:areaId/seguimiento`.

1. Durante la formulación, consultar el consolidado sin campos editables. Al abrir Secretaría el período del año siguiente, elegir **Fecha de control** y pulsar **Abrir fecha de control**.
2. Consultar el resumen y las filas con indicador, objetivo, meta, bloque de actividades y plazos del plan.
3. Elegir estado, escribir porcentaje y observaciones, y pulsar **Guardar avance de esta fila**. Guardar una fila conserva ediciones pendientes de otras.
4. Completar los tres campos y guardar todas las filas en una misma fecha. Pulsar **Finalizar seguimiento del área** y confirmar. Repetir en las demás áreas pendientes del año anterior para habilitar la nueva autoevaluación.
5. Si falla una consulta se conservan los datos visibles y la edición queda en pausa hasta reintentar; si falla un guardado, se conservan las respuestas y se permite reintentar.

Se usan las variables SEM y la tipografía de Antonio. El encabezado azul separa cada fila y el resumen presenta una tabla compacta adaptable a celular. Los campos del plan son de consulta y se editan en Anexo 3. Reiniciar el backend para cargar V15 y las nuevas rutas antes de probar.

## Verificación

Verificación del 24 de octubre: suite completa del backend con PostgreSQL local, **126 pruebas correctas**, incluidas 10 de `PMIFollowUpTests`. Comprueban lectura sin escrituras, varias filas y bloques de actividades, cinco estados manuales, porcentaje exacto y pendiente/cero, borradores, controles independientes e idempotencia por fecha/fila, instantáneas ante cambios y eliminación del origen, selección vigente, límites y permisos por institución/área/año y Secretaría. Flyway aplicó V15 y Hibernate validó el esquema. Compilación Vue/TypeScript y ESLint correctos.

El recorrido de navegador con datos aislados verificó cinco filas de origen, ausencia de estados o porcentajes automáticos, guardado de una fila sin reemplazar otra edición pendiente, porcentajes con coma, validación del rango, reintento sin perder respuestas, fechas independientes y recuperación del historial. También comprobó contexto conservado ante cambios y retirada del origen, aviso antes de descartar respuestas, recuperación de consulta, recarga, estado vacío y ancho móvil de 390 px sin desbordamiento horizontal. No modificó información institucional real.

Verificación de la corrección del ciclo del 26 de octubre: suite completa con **136 pruebas correctas**, incluyendo consulta sin escritura durante formulación, apertura por el período siguiente, rechazo de creación/valoración/envío sin seguimiento anterior finalizado, tres campos obligatorios, finalización explícita e idempotencia después del cierre, obligatoriedad en todas las áreas con PMI previo e independencia institucional. Comprueba la revalidación cuando cambia el plan y los permisos de consulta y finalización. La migración V16 y el esquema JPA fueron validados con PostgreSQL local.

Compilación Vue/TypeScript y ESLint correctos. En navegador con datos aislados se comprobó el consolidado sin indicador repetido ni campos editables durante formulación; proceso, componente, objetivo y meta visibles; bloqueo en panel y enlace directo sin cargar las valoraciones nuevas; finalización después de guardar las tres respuestas de todas las filas; consulta posterior y aparición de las cuatro entradas de autoevaluación. Un fallo al verificar requisitos mantiene bloqueado el formulario. Se revisó a 390 px sin desbordamientos. La regresión de ayuda/evidencia mantuvo el campo único, autoguardado, recarga y envío. No se modificaron datos institucionales reales.
