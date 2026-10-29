# PMI: ficha de indicadores del Anexo 4

Implementación del 24 de octubre de 2026 en `JuanM`. Fuente: hoja **Anexo 4**, «Definición de los indicadores de resultados», del Excel de evidencias `24. IEM CIUDADELA EDUCATIVA PASTO AUTOEV 2025 Y PMI 2026-2028 G. ACADEMICA.xlsx`. Su numeración corresponde al Excel local, no a los anexos de la Guía 34 actualizada.

## Reglas y trazabilidad

| Regla / datos | Actor | Fuente | Validación y criterio de aceptación |
| --- | --- | --- | --- |
| Una tabla por indicador del Anexo 3 | Institución | Solicitud de Juan del 24 de octubre; tablas A–B, G–H y J–K del Excel | Cinco indicadores con nombre guardados producen cinco tablas. No se fija el número de tablas ni se fusionan indicadores que tienen el mismo nombre. |
| Nombre y contexto de origen | Institución | Anexos 3 y 4 del Excel; relación Objective → Goal → Activity / Indicator | El nombre se consulta del mismo `Indicator`, sin copiarlo. Cada ficha identifica área, proceso, componente, objetivo y meta. Renombrar la fila conservando su UUID mantiene la definición. |
| Tipo: proceso o resultado | Institución | Excel A13/B13 y A39/B39 y equivalentes; Guía 34 de 2026, pp. 87–88 | La institución elige el tipo. No se asigna automáticamente por componente, estado o nombre. Se rechazan tipos desconocidos. |
| Objetivo, unidad, variables, fórmula, fuentes, metodología, periodicidad y responsable de medición | Institución | Excel A14:B21 y tablas equivalentes; Guía actualizada, pp. 88–89 | Se conservan como textos; la fórmula no se ejecuta. Definir variables y fórmula son campos separados. La periodicidad y responsable de medición no sobrescriben la recolección ni el responsable de actividades del Anexo 3. |
| Borradores parciales | Institución | Continuidad con el borrador del Anexo 3; decisión técnica de esta iteración | Se puede guardar una ficha incompleta. Vacío se normaliza a `null`. El indicador «completa» solo informa si los campos de la ficha guardada tienen contenido; no envía ni cierra el PMI ni valida la calidad pedagógica de la respuesta. |
| Separación institucional y por área / año | Institución / Secretaría | Flujo del equipo y decisión previa: un PMI por área | Solo escribe la institución propietaria. Secretaría puede consultar con `establishmentId`, sin editar respuestas institucionales. UUID ajenos, de otra área o de otro año no son aceptados. |
| Históricos y selección vigente | Institución | Continuidad con Anexos 2 y 3 | Una ficha de un objetivo que pierde selección/elegibilidad permanece visible para consulta y vuelve a ser editable al recuperar la condición. Quitar explícitamente una fila/meta/objetivo en el Anexo 3 elimina sus indicadores y fichas; se informa en la confirmación. |

Referencia oficial: [Guía 34 actualizada de 2026, paso de definición de indicadores, pp. 86–90](https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf). El Excel remite a pp. 58–61 de una edición anterior: esa referencia se conserva como procedencia del formato, sin atribuirla a la paginación de 2026.

## Persistencia y diagrama

V14 amplía `pmi_indicator` con los datos de la ficha, dejando intactos los UUID, nombres y relaciones existentes de V10. Se mantiene `Objective → Goal → Activity → Indicator`, que permite varios indicadores por meta según el Excel. No se crean copias ni registros nuevos al consultar el Anexo 4.

Correspondencia con `Indicator` del diagrama: `description` es el objetivo del indicador, `type` su tipo, `measureUnit` la unidad, `dataFormula` (normaliza la grafía `dataFormule`) la fórmula, `dataSource` las fuentes y `calculationFrequency` la periodicidad. Se agregan `variablesDefinition`, `methodologicalAspects` y `measurementResponsible` para los campos explícitos del Excel. El avance de ejecución se registra por separado en Anexo 5; las observaciones de Secretaría y la medición numérica de las fórmulas siguen pendientes.

Las filas sin nombre no generan tablas vacías: se cuenta cuántas requieren completar el nombre en el Anexo 3. Si se vacía el nombre de un indicador cuya ficha ya tiene datos, su tabla se conserva para consulta; restaurar el nombre en la misma fila permite continuar sin perder información.

Límites técnicos: 500 caracteres para unidad, periodicidad y responsable; 10.000 para los demás textos. La base de datos y la validación de entrada aplican esos límites. No son restricciones municipales de contenido. Guardar la ficha coordina el mismo bloqueo de autoevaluación que la priorización y formulación, para evitar escrituras simultáneas incoherentes con cambios de selección, nombre o eliminación.

## API

| Método y ruta | Actor | Respuesta |
| --- | --- | --- |
| `GET /api/pmi/{year}/areas/{areaId}/indicators` | Institución propia; Secretaría con `?establishmentId=UUID` | Cabecera del área, `canEdit`, número de filas sin nombre y fichas en orden de objetivo, meta y fila. La consulta vacía no crea un PMI. |
| `PUT /api/pmi/{year}/areas/{areaId}/indicators/{indicatorId}` | Institución propia | Guarda la definición completa de una ficha existente y devuelve solo esa ficha. Requiere nombre guardado en Anexo 3 y objetivo editable según la selección actual. |

Ejemplo de cuerpo de guardado:

```json
{
  "type": "PROCESS",
  "description": "Medir el avance de las sesiones programadas",
  "measureUnit": "Porcentaje (%)",
  "variablesDefinition": "R: sesiones realizadas\nP: sesiones programadas",
  "dataFormula": "(R / P) * 100",
  "dataSource": "Actas y cronograma institucional",
  "methodologicalAspects": "Contar las sesiones con acta y comparar con el cronograma",
  "calculationFrequency": "Trimestral",
  "measurementResponsible": "Coordinación académica"
}
```

`type` acepta `PROCESS`, `RESULT` o `null`. Los campos omitidos o vacíos quedan pendientes: `PUT` sustituye los datos de esa ficha. El nombre se modifica exclusivamente en Anexo 3. Cada ficha devuelve su `id` de indicador, `rowId`, `objectiveId`, `goalId`, contexto, `editable`, `complete` y `definition`.

## Interfaz y prueba manual

1. En el Anexo 3 del área, escribir los nombres de los indicadores y pulsar **Guardar este plan**.
2. Abrir **Definir las fichas de los indicadores →**, al final de los planes. Ruta: `/pmi/:year/areas/:areaId/indicadores`.
3. Verificar una tabla por indicador, con nombre de origen y contexto; el índice permite saltar a cualquiera.
4. Completar y pulsar **Guardar ficha del indicador**. Guardar una no sustituye respuestas pendientes de las otras.
5. Recargar para verificar persistencia. Si falla el guardado, las respuestas siguen en pantalla y se puede reintentar. Salir o actualizar con cambios pendientes solicita descartarlos; se impide salir durante un guardado en curso.
6. Cambiar el nombre en el Anexo 3 conservando la fila, guardar y volver al Anexo 4: nombre actualizado y definición conservada. **Actualizar indicadores** vuelve a consultar el origen, sin duplicar fichas.

Las tablas usan las variables SEM y la tipografía de Antonio, con encabezados azules y disposición adaptable. No incorporan cambios de paleta global. Para probar desde el entorno local, reiniciar el backend para aplicar V14 y mantener el frontend en ejecución.

La sección **Seguimiento del plan · Anexo 5** enlaza al [registro de avances por fecha](pmi-anexo5.md). Completar toda la ficha no es una condición adicional para registrar ejecución; el Anexo 5 utiliza las filas del Anexo 3 y conserva su propia información.

## Verificación

Verificación del 24 de octubre: suite completa del backend con PostgreSQL local, **116 pruebas correctas**, incluidas 10 de `PMIIndicatorTests`. Comprueban cinco fichas entre varias metas/componentes, nombres repetidos, persistencia de todos los campos, separación de la recolección del Anexo 3, renombrado sin pérdida, filas sin nombre, borradores, validaciones, permisos por rol/institución/área/año, conservación histórica y eliminación explícita sin indicadores huérfanos. Flyway aplicó V14 y Hibernate validó el esquema. Compilación Vue/TypeScript y ESLint correctos.

El recorrido de navegador con datos aislados verificó cinco tablas, carga sin escrituras, guardado independiente sin sustituir otra edición pendiente, recarga y persistencia, reintento tras error sin perder texto, actualización del nombre sin duplicados, históricos solo para consulta, recuperación de consulta, aviso antes de descartar y estado vacío por área. La vista de 390 px conserva las tablas sin desbordamiento horizontal y todos los controles tienen etiquetas asociadas. Esta verificación visual no escribe datos institucionales reales ni sustituye las pruebas de persistencia.
