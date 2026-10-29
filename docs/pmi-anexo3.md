# PMI: formulación del Anexo 3

Implementación del 21 de octubre de 2026 en `JuanM`. Fuente principal: hoja **Anexo 3**, «Formulación del Plan de Mejoramiento Institucional», del Excel de evidencias `24. IEM CIUDADELA EDUCATIVA PASTO AUTOEV 2025 Y PMI 2026-2028 G. ACADEMICA.xlsx`. La numeración corresponde a las hojas de ese archivo, no a los anexos del PDF de la Guía 34 de 2026. Complementa la [priorización del Anexo 2](pmi-anexo2.md).

## Trazabilidad

| Regla o dato | Actor | Fuente | Validación y resultado esperado |
| --- | --- | --- | --- |
| Proceso y componente por objetivo | Institución | Columnas A–C; celdas combinadas A13:A21, B13:B21 y C13:C21; aclaración de Juan | Se toma un componente de la selección vigente del Anexo 2 del área: máximo U/T/I, hasta dos, después de completar y enviar la autoevaluación del área y puntuar todos los candidatos. No se digitan nombres libres. |
| Objetivo con varias metas | Institución | C13:C21 y D13:D15, D16:D18, D19:D21 | La estructura permite varias metas de texto por objetivo. |
| Varios indicadores por meta | Institución | E13:E21 frente a las combinaciones de D | Cada meta admite varias filas; el indicador contiene aquí solo su nombre. |
| Actividades de la fila | Institución | Columna F | Se conserva un bloque de texto por fila. No se separa automáticamente en actividades atómicas ni se eliminan bloques repetidos. |
| Costo, FSE y otra fuente | Institución | Columnas G–I; fila de totales 22 | Importes opcionales, no negativos y de hasta dos decimales; cero y pendiente se conservan distintos. Los ejemplos suman COP 3.000.000. No se impone igualdad entre costo y financiación, pues el formato no establece esa validación. |
| Responsable y fechas | Institución | Columnas J–L | Textos y fechas opcionales en borrador; si están ambas fechas, fin no puede ser anterior a inicio. No se fuerza el año de la autoevaluación: el ejemplo proyecta actividades en años siguientes. |
| Instrumentos y frecuencia de recolección | Institución | Columnas M–N | Textos opcionales por fila. El avance de ejecución se registra aparte en Anexo 5. |
| Un objetivo por gestión y hasta cuatro actividades | Institución | Orientaciones del encabezado del Excel | Se muestran como recomendaciones; no son límites duros. |
| Consulta sin edición institucional ajena | Secretaría | Separación de actores del flujo del equipo | Consulta mediante `establishmentId`; no crea, modifica ni elimina respuestas institucionales. |
| Borrador y conservación histórica | Institución | Decisión de implementación para continuidad con Anexo 2 | Crear/editar exige selección válida y componente seleccionado elegible. Una pérdida de elegibilidad o selección conserva el objetivo para consulta; eliminarlo exige una acción explícita. |

Los campos monetarios usan `NUMERIC(16,2)`: hasta 14 cifras enteras. Es un límite técnico de almacenamiento, no un monto máximo municipal confirmado. Las respuestas monetarias son cadenas decimales con dos posiciones para conservar precisión en JavaScript. Los totales suman únicamente objetivos cuyo componente sigue seleccionado y elegible; cuentan por separado importes vacíos y objetivos históricos excluidos. No convierten un campo pendiente en cero guardado.

## Correspondencia con el diagrama

V10 incorpora `Objective` → `PrioritaryComponent`, `Goal` → `Objective` y `Activity` → `Goal`. Cada fila `Activity` tiene un `Indicator` propio y conserva actividades, recursos, responsable, fechas y recolección. Las metas y filas conservan su orden explícito.

El diagrama dibuja un indicador por meta; el Excel contiene varios. Se amplía esa relación mediante las filas, sin perder el vínculo objetivo → meta → actividades/indicador. `Activity.description` representa el bloque de actividades de una fila del Excel. La ficha detallada se diligencia en [Anexo 4](pmi-anexo4.md) y el avance de ejecución por fecha en [Anexo 5](pmi-anexo5.md). Las observaciones de Secretaría, la medición numérica de las fórmulas y el envío/cierre formal siguen pendientes.

## API

| Método y ruta | Permiso | Resultado |
| --- | --- | --- |
| `GET /api/pmi/{year}/areas/{areaId}/formulation` | Institución propia; Secretaría con `?establishmentId=UUID` | Formulación del área, selección, objetivos, metas, filas y totales. La consulta vacía no crea un PMI. `selfEvaluationSubmitted` y `submittedAt` identifican su entrega. |
| `POST /api/pmi/{year}/areas/{areaId}/formulation/objectives` | Institución propia | Crea un objetivo y devuelve toda la formulación del área. |
| `PUT /api/pmi/{year}/areas/{areaId}/formulation/objectives/{objectiveId}` | Institución propia | Guarda un objetivo y su estructura completa; devuelve toda la formulación del área. |
| `DELETE /api/pmi/{year}/areas/{areaId}/formulation/objectives/{objectiveId}` | Institución propia | Elimina explícitamente el objetivo y sus hijos; devuelve `204`, incluso si es histórico. |

Ejemplo de cuerpo de creación (UUID ilustrativo: sustituir por un componente realmente seleccionado):

```json
{
  "componentId": "00000000-0000-0000-0000-000000000001",
  "description": "Fortalecer las prácticas de evaluación",
  "goals": [{
    "description": "Acordar criterios comunes durante el año",
    "rows": [{
      "indicatorName": "Criterios de evaluación acordados",
      "activities": "Revisar los criterios actuales\nRealizar una jornada de trabajo",
      "totalCost": "1500000.00",
      "fseAmount": "1500000.00",
      "otherAmount": null,
      "responsible": "Equipo académico",
      "startDate": "2027-02-03",
      "finishDate": "2027-04-30",
      "collectionInstruments": "Actas de las jornadas",
      "collectionFrequency": "Mensual"
    }]
  }]
}
```

En una actualización deben enviarse los `id` existentes de metas y filas para conservarlos; los nuevos se omiten o son `null`. Quitar una meta/fila del cuerpo de un `PUT` la elimina junto con sus hijos. Se rechazan IDs ajenos, repetidos o reutilizados en una creación antes de modificar la estructura. Los arreglos `goals` y `rows` son obligatorios y pueden estar vacíos. El texto vacío se normaliza a `null`.

Los textos de objetivo, meta, actividades e instrumentos admiten 10.000 caracteres; nombre del indicador, responsable y frecuencia, 500. Son límites técnicos. El borrador no exige completar esos campos. Las operaciones se coordinan con el bloqueo de la autoevaluación usado por Anexo 2 para mantener coherentes selección y formulación.

## Pantalla y comprobación

Desde el PMI del área (`/pmi/:year/areas/:areaId`), abrir **Elegir componentes y preparar el plan · Anexo 3** tras guardar los U/T/I de todos los candidatos. La ruta es `/pmi/:year/areas/:areaId/formulacion`. La pantalla se organiza en dos pasos: elegir los componentes de mayor puntaje y pulsar **Generar formularios**; después completar y guardar el plan de cada componente. Se conservan las ediciones pendientes de otros objetivos al guardar o eliminar uno. Quitar hijos guardados y eliminar objetivos solicita confirmación. El resumen de recursos se consulta debajo de los formularios y corresponde a los datos guardados del área.

### Simplificación para docentes del 23 de octubre de 2026

Fuente: solicitud expresa de Juan para facilitar el diligenciamiento. Las tarjetas del primer paso muestran componente, proceso y U/T/I. Conservan la regla acordada: un máximo único produce un formulario; dos máximos producen dos; con más de dos empatados se eligen exactamente dos. Solo se ofrecen los componentes del máximo vigente, sin añadir componentes de menor puntaje.

**Generar formularios** prepara por componente un objetivo con una meta y un indicador/actividades, dejando disponibles todos los campos de responsable, fechas, costos, fuentes de financiación y recolección de información. Son espacios de borrador: no se crean objetivos vacíos en la base de datos hasta pulsar **Guardar este plan**. Pueden añadirse más metas e indicadores; agregar otro objetivo queda como una opción secundaria. Los campos se agrupan mediante preguntas para facilitar su comprensión.

Volver a generar reutiliza los formularios y registros existentes. No duplica los objetivos ni reemplaza textos, UUID, metas o filas ya diligenciados; completa únicamente los espacios iniciales que falten. Se conserva el contenido de componentes históricos para consulta. Antes de cambiar de componentes, se deben guardar o descartar explícitamente las ediciones pendientes.

La selección puede resolverse desde Anexo 2 o desde estas tarjetas del Anexo 3. Ambas usan el mismo `PUT /api/pmi/{year}/areas/{areaId}/selection`. El acceso a elegir componentes exige autoevaluación completa y enviada y priorización terminada; crear/editar objetivos sigue exigiendo la selección válida confirmada por el servidor (`canFormulate`). Después de guardar una selección, la pantalla vuelve a consultar la formulación antes de habilitar la edición. Si falla esa consulta, conserva los formularios y permite reintentar con **Generar formularios**.

Verificación del 23 de octubre: compilación Vue/TypeScript y ESLint correctos. El recorrido de navegador con datos aislados comprobó elección de dos entre tres máximos, preparación de todos los campos sin crear objetivos vacíos en la base, generación repetida sin duplicados, conservación de IDs y respuestas al continuar, bloqueo de cambios de selección con ediciones pendientes, recuperación si falla la consulta después de guardar una selección e históricos para consulta. También se verificaron uno y dos máximos automáticos y ancho móvil de 390 px sin desbordamiento. No se modificaron datos institucionales reales durante esa comprobación.

La aclaración de Juan del 21 de octubre modifica la selección inicial: uno o dos máximos U/T/I pasan automáticamente; si hay tres o más, la institución elige exactamente dos. No existe un umbral fijo de 15 ni se añade un componente de menor puntuación. `canFormulate` exige completar y enviar la autoevaluación del área, puntuar todos sus candidatos y tener resuelta esa selección. Las consultas históricas y la eliminación explícita se conservan aunque falte esa condición. V11 separa planes anteriores por área sin eliminar sus objetivos ni duplicar sus hijos.

Los estilos son locales y usan las variables y tipografía SEM incorporadas por Antonio. El PMI continúa disponible después del cierre de autoevaluación; un período propio para esta etapa todavía no está definido.

Las pruebas `PMIFormulationTests` cubren persistencia real, varias filas por meta, importes exactos y pendientes, conservación de IDs, eliminación de hijos, selección e históricos, fechas y permisos por institución/Secretaría. La compilación Vue/TypeScript y ESLint comprueban el frontend. Reiniciar el backend para aplicar V10 antes de probar la pantalla.

Verificación del 21 de octubre tras la separación por áreas: suite completa de backend con PostgreSQL local, **89 pruebas correctas**, incluidas 14 de formulación, 21 de priorización y una migración con datos anteriores de dos áreas; compilación Vue/TypeScript y ESLint correctos. La comprobación de navegador con respuestas de prueba aisladas verificó las rutas por área, tres indicadores por meta, conservación de otra edición pendiente, precisión del importe máximo, distinción entre cero y vacío, consulta histórica y ancho móvil de 390 px sin desbordamiento. Esta comprobación visual no utiliza datos institucionales reales ni sustituye las pruebas de persistencia anteriores.
