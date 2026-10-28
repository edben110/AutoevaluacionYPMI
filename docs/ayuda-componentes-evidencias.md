# Ayuda del componente y referencias de evidencia

Solicitud de Juan del 21 de octubre de 2026: consultar desde la autoevaluación qué evalúa cada componente y ejemplos pertinentes de evidencia; aceptar enlaces o texto en el mismo recuadro cuando los soportes no estén digitalizados. Juan precisó que desea **solo la descripción del ítem**, sin incorporar los cuatro descriptores de valoración a la ayuda.

## Fuente y alcance

Se contrastó la [Guía 34 actualizada, Ministerio de Educación Nacional, 2026](https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf). El anexo 1 contiene los componentes y sus descriptores (pp. 115–163). La aplicación muestra una síntesis breve de qué revisar, redactada para cada uno de los 45 componentes del catálogo V6; no reproduce los cuatro niveles ni cambia la elección manual de estado.

El anexo 3 presenta fuentes de evidencia **por proceso**, no una lista exclusiva de documentos aceptados por componente. Su introducción (p. 168) permite seleccionar otras evidencias pertinentes según el contexto y la disponibilidad institucional. Las dos sugerencias de cada componente son una selección editorial a partir de esas fuentes; se identifican como ejemplos orientadores, no obligatorios ni exhaustivos. La aplicación no agrega requisitos documentales al guardado ni al envío.

| Regla | Actor | Fuente | Validación y resultado |
| --- | --- | --- | --- |
| Consultar qué evalúa el componente | Institución; Secretaría consulta por API | Aclaración de Juan; anexo 1 | Botón de ayuda por componente, con una descripción breve y página de referencia. No modifica respuestas ni selecciona estados. |
| Mostrar ejemplos pertinentes | Institución | Anexo 3, pp. 168–175; aclaración de la reunión | Filtra por la relación componente/proceso, muestra fuente y páginas y advierte que no son exigencias. |
| Mantener el contexto administrativo y financiero | Institución | Anexo 1, pp. 145–154; anexo 3, pp. 172–174 | Las diez ayudas se diferencian por componente; no se entrega el listado financiero a todos los componentes del área. |
| Describir evidencias físicas | Institución | Decisión municipal comunicada por Juan; gestión documental, p. 146 | El mismo recuadro admite texto, enlaces, saltos de línea o una combinación. Puede referir documento, fecha y lugar de consulta sin exigir escaneo. |
| Conservar respuestas y permisos | Institución; Secretaría consulta | Contratos vigentes | Mantiene guardado automático, envío independiente por área, consulta posterior y bloqueo de modificación de áreas enviadas. |

## Cuidado de la pertinencia en el área administrativa y financiera

Los componentes 28–37 del catálogo tienen su propia descripción en las pp. 145–154. Las ayudas se limitan al asunto correspondiente:

| Proceso | Orientación de las sugerencias | Fuente de evidencia |
| --- | --- | --- |
| Cobertura educativa | Matrícula/novedades y, por separado, organización y consulta del archivo | pp. 172–173 |
| Planta física y recursos | Mantenimiento/uso de espacios y, por separado, disponibilidad y aprovechamiento de equipos | p. 173 |
| Talento humano | Asignación e inducción, seguimiento del desempeño, formación o bienestar, según el componente | p. 173 |
| Apoyo financiero y contable | Presupuesto y registros contables para gestión del FSE; informes compartidos y participación para rendición de cuentas | p. 174 |

Esta selección no convierte auditorías, expedientes completos o documentos ajenos al componente en condiciones de diligenciamiento. La institución conserva la elección de soportes adecuados a su situación. Las restricciones municipales adicionales deben registrarse con su fuente antes de incorporarlas como validaciones.

## Datos y API

`backend/src/main/resources/guidance/guia34-2026.json` contiene 45 entradas con UUID y nombre del componente, UUID del proceso, descripción breve, dos ejemplos y páginas. Se conserva como recurso versionado junto al código, sin modificar los registros de catálogo existentes ni distribuir el PDF completo.

`GET /api/components/{componentId}/guidance` permite consulta autenticada de institución y Secretaría. Devuelve `componentId`, `componentName`, `areaName`, `processName`, `evaluationItem`, `evidenceExamples`, `evidenceNotice`, `sourceTitle`, `sourceUrl`, `itemPages`, `evidencePages` y `officialGuidance`.

La correspondencia con la guía exige coincidir en UUID, proceso y nombre. Un componente nuevo, renombrado o trasladado usa su descripción/evidencia configurada en el catálogo y no se atribuye a páginas oficiales que ya no le correspondan. Si carece de esos textos, la ayuda lo indica; no inventa requisitos. Las páginas informadas son impresas; el PDF tiene una portada adicional en la numeración física del visor.

## Evidencia como enlace o texto

Se mantiene el nombre JSON **`evidenceUrl`** por compatibilidad con los clientes existentes. A partir de V13 representa una **referencia de evidencia**, no únicamente una URL:

- Admite hasta 20.002 caracteres y saltos de línea; sigue siendo opcional. Este límite técnico permite reunir los dos campos anteriores de 10.000 caracteres con un separador sin truncar su contenido.
- Conserva enlaces anteriores al convertir `evidence_url` de `VARCHAR(1000)` a `TEXT`.
- Recorta espacios exteriores y normaliza el vacío a `null`. Por solicitud de Juan del 26 de octubre, la interfaz presenta un único campo **Evidencias: enlaces o descripción**. Al cargar combina `evidenceUrl` y la nota anterior `evidenceNote`, separados por una línea en blanco. La lectura no escribe ni altera datos. Al guardar una respuesta desde esta interfaz, envía el texto unificado en `evidenceUrl` y limpia `evidenceNote` para que no vuelva a duplicarse al recargar. El campo legado sigue aceptado por la API para compatibilidad con clientes anteriores, con su límite de 10.000 caracteres.
- No valida protocolos ni interpreta el contenido como HTML o hipervínculo. Una referencia escrita se presenta como texto.
- Usa el guardado automático y la verificación de pendientes antes de enviar ya existentes.

Ejemplo de solicitud:

```json
{
  "level": 2,
  "evidenceUrl": "Archivo físico: acta 12 de 2026, folios 30 a 70. Consultar en coordinación.\nSoportes complementarios: https://ejemplo.org/carpeta",
  "evidenceNote": null
}
```

En la pantalla, **Ayuda del componente** abre un diálogo con carga, reintento, cierre por teclado y retorno del foco. Está disponible también cuando el área ya fue enviada. La ayuda se muestra dentro de la aplicación; abrir el PDF es opcional. Solo los enlaces de fuente documental se presentan como enlaces, separados del contenido escrito por la institución.

La unificación es una simplificación del formulario solicitada por Juan, sin añadir exigencias documentales ni cambiar los permisos de envío/consulta. Las áreas ya enviadas muestran ambos textos anteriores juntos, sin modificarlos en la base de datos. La columna `evidence_url` ya es `TEXT` desde V13: no se necesita migración ni se borran columnas o notas históricas.

Verificación de la unificación del 26 de octubre: **131 pruebas de backend correctas**, compilación Vue/TypeScript y ESLint correctos. La persistencia acepta los dos textos anteriores completos (20.002 caracteres con separador) en el campo unificado y rechaza una referencia superior al nuevo límite. El recorrido de navegador comprobó un único recuadro por componente, visualización conjunta sin guardar al cargar, guardado con nota legada vacía, recarga sin duplicados, preservación de los textos largos, envío y consulta posterior. También mantuvo las comprobaciones de ayuda y ancho móvil de 390 px. No se modificaron datos institucionales reales durante la verificación.

Reiniciar el backend para aplicar V13 y cargar el recurso antes de probar. Las pruebas `ComponentGuidanceAndEvidenceTests` verifican la cobertura de los 45 componentes, páginas financieras, consultas por rol, fallback del catálogo, referencias físicas/mixtas/largas y rechazo de exceso de longitud sin reemplazar el valor anterior. Las pruebas previas verifican que los enlaces existentes y las reglas de envío se conservan.

Verificación del 21 de octubre: 96 casos de regresión y los diez casos nuevos de ayuda/evidencia comprobados con PostgreSQL local; el caso de texto largo se volvió a ejecutar tras ajustar la expectativa de recorte de espacios, sin cambios de producto. Compilación Vue/TypeScript y ESLint correctos. En navegador con respuestas aisladas se comprobaron ayuda sin escrituras, descripción sin los cuatro criterios, fuentes y páginas, Tab/Shift+Tab/Escape y retorno de foco, error/reintento y descarte de una respuesta tardía de otro componente. También se verificaron evidencia mixta de más de 1.000 caracteres, saltos de línea y texto literal sin interpretación HTML, guardado automático, recarga, envío y ayuda en área de consulta. El diálogo y la página no desbordan a 390 px. Las pruebas visuales no modifican datos institucionales reales.
