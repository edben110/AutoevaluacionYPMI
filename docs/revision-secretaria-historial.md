# Revisión de Secretaría e historial institucional

Decisión de Juan del 27 de octubre de 2026: Secretaría recibe la autoevaluación enviada y consulta el PMI de cada institución y área. Los comentarios se registran **solo por componente de la autoevaluación**; el PMI tiene consulta sin comentarios de Secretaría. La institución conserva acceso a la entrega anterior y a esas observaciones al abrir el siguiente período, junto con el seguimiento pendiente de su PMI.

**Corrección de Juan del 28 de octubre:** la consulta de PMI en esta documentación queda limitada al PMI **finalizado y enviado explícitamente** por su institución. Los borradores ya no aparecen ni son consultables por Secretaría, incluso con enlace/API directos. Guardar un borrador no cambia la revisión del paquete; enviarlo sí. Los anexos 2–4 enviados quedan de consulta y el Anexo 5 conserva su ciclo de seguimiento. La sección siguiente refleja el alcance inicial; esta corrección prevalece sobre las referencias a avances incompletos. Véase [envío por área](envio-pmi-por-area.md).

Fuentes de contexto: flujo de la wiki (seleccionar institución, revisar componentes/evidencias y observar), aclaración del equipo en `estado-proyecto.md` (observaciones para la siguiente entrega, sin modificar respuestas ya enviadas) y Guía 34 de 2026, etapas de autoevaluación (pp. 49–74) y seguimiento (pp. 97–114). La granularidad de comentarios y los permisos siguientes son decisiones del proyecto; no se atribuyen como restricciones municipales de la guía.

| Regla | Actor y fuente | Datos | Validación / aceptación |
| --- | --- | --- | --- |
| Recibir áreas enviadas | Secretaría; Juan y flujo wiki | Institución, DANE, año, área, fecha de envío | La bandeja excluye borradores; buscar por institución/área y filtrar por año. |
| Consultar respuestas sin editarlas | Secretaría; Juan | Estado, evidencias, fortalezas y oportunidades por proceso/componente | No hay controles de edición institucional. Los endpoints de escritura mantienen el rol institucional. |
| Observar por componente | Secretaría; corrección explícita de Juan | Texto, componente valorado, entrega, autor y fecha | Texto no vacío de hasta 10.000 caracteres, autor obtenido de la sesión; solo componentes de esa entrega enviada. |
| Conservar separación y trazabilidad | Equipo y diagrama de clases | `ComponentReviewComment` en tabla independiente | No cambia `ComponentValuation`, su estado, fechas o evidencia. Las observaciones se agregan conservando las anteriores; no implican aprobación, rechazo ni reapertura. |
| Consultar PMI | Secretaría; corrección explícita de Juan | Anexos 2–5 guardados por institución y área | Solo lectura, incluidos apartados incompletos y controles históricos. No se añade comentario ni un estado de entrega final del PMI que el equipo aún no ha definido. |
| Consultar historial propio | Institución; Juan | Entregas de años anteriores, observaciones y PMI | Funciona fuera de las fechas de diligenciamiento y mientras el seguimiento anterior bloquea la nueva autoevaluación. No permite consultar otra institución. |
| Mantener el requisito del ciclo siguiente | Institución; Juan del 26 y 27 de octubre | Seguimiento previo por área | El enlace a la autoevaluación anterior acompaña al seguimiento; consultar comentarios no sustituye su finalización obligatoria. |

## Contratos

- `GET /api/reviews/deliveries`: Secretaría recibe las entregas de todas las instituciones; una institución recibe exclusivamente las propias. No expone usuarios, correos de acceso ni credenciales.
- `GET /api/reviews/{year}/areas/{areaId}`: entrega enviada y componentes valorados con sus nombres/descripciones y procesos, incluidos los que posteriormente se desactiven. Secretaría debe indicar `?establishmentId=UUID`; la institución usa su sesión y recibe 403 si solicita otra institución.
- `GET /api/reviews/{year}/areas/{areaId}/comments`: observaciones de esa entrega con componente, autor y fecha. Misma pertenencia y selector.
- `POST /api/reviews/{year}/areas/{areaId}/components/{componentId}/comments?establishmentId=UUID`: solo Secretaría; cuerpo `{ "body": "Observación" }`. Bloquea la autoevaluación durante el registro para serializar con su reinicio/eliminación de PMI. No requiere que el período siga abierto.

La migración V17 crea `component_review_comment`, asociada a la entrega por área y a la valoración. No migra ninguna respuesta ni inventa observaciones. Al abrir un año diferente, las observaciones anteriores permanecen. Eliminar un período conserva la autoevaluación y sus observaciones para consulta, pero elimina sus PMI según la decisión anterior de Juan. **Eliminar y volver a publicar el mismo año** reinicia sus respuestas y envíos; las observaciones de esas entregas también se eliminan por sus claves foráneas en cascada y no reaparecen en la nueva entrega. Esta acción se distingue de abrir el siguiente año.

## Interfaz

Secretaría accede desde **Revisar entregas de las instituciones**. La institución accede desde **Mis autoevaluaciones e historial**, disponible incluso sin período abierto. La bandeja agrupa en paquetes por institución y año; al abrir cada paquete se consultan sus áreas. La pantalla por área separa **Autoevaluación y comentarios** de **Consultar PMI · Anexos 2 a 5**. Los enlaces HTTP/HTTPS de evidencia abren en otra pestaña y el texto de documentos físicos se conserva. Los textos institucionales y comentarios se muestran como texto escapado, sin HTML ejecutable.

La vista del PMI muestra el avance guardado: matriz U/T/I, objetivos/metas/actividades y recursos, fichas de indicadores y controles de seguimiento por fecha. Los controles de selección de anexo/fecha solo consultan; no guardan ni presentan formularios institucionales a Secretaría. La institución puede pasar de la consulta al formulario existente de seguimiento, que conserva sus validaciones de período y finalización.

## Paquetes y marca de revisión

Ampliación solicitada por Juan el 27 de octubre: un espacio por nombre de institución que reúna autoevaluaciones y PMI y permita marcar lo revisado. El paquete corresponde a la autoevaluación anual existente, sin unir los cuatro PMI en uno ni modificar las entregas por área. Aparece al recibir la primera área y muestra cuántas autoevaluaciones llegaron y cuántos PMI se iniciaron. No se exige que existan cuatro PMI: un área con todos los componentes en Mejoramiento continuo puede no necesitarlo.

| Regla | Fuente / actor | Datos | Validación / aceptación |
| --- | --- | --- | --- |
| Agrupar por institución y año | Solicitud de Juan; Secretaría e institución propia | Nombre, DANE, año, áreas recibidas y PMI | Una tarjeta por paquete, sin tarjetas separadas por cada área. Buscar por institución, DANE o área; filtrar por año y revisión. |
| Marcar lo revisado | Solicitud de Juan; solo Secretaría escribe | Marca persistida, autor, fecha, versión de lo recibido | **Marcar paquete como revisado** actualiza el paquete; un reintento de la misma versión conserva autor y fecha originales. La institución consulta la marca. |
| Corregir una marca | Acción reversible de la interfaz | **Volver a pendiente** | No borra respuestas, PMI ni comentarios; solo retira la marca. |
| Evitar una revisión desactualizada | Decisión de implementación para la marca | Huella de áreas recibidas y cambios guardados en PMI | Si llega otra área o cambia el PMI, se muestra pendiente con aviso y fecha de la última revisión. El servidor rechaza marcar una versión que ya cambió desde que se cargó. |

La marca describe lo recibido hasta ese momento; no representa aprobación del contenido, no reabre autoevaluaciones y no habilita por sí sola el año siguiente. Los comentarios continúan solo por componente de autoevaluación. No cambia las fechas, permisos ni finalización obligatoria del seguimiento.

V18 crea `DeliveryPackageReview`, asociado de manera única a `SelfEvaluation`, separado de los estados institucionales y comentarios. Al reiniciar el mismo año por eliminación y nueva publicación del período también se retira la marca. Abrir otro año conserva el historial del anterior y el nuevo paquete empieza pendiente.

- `GET /api/reviews/packages`: paquetes de todas las instituciones para Secretaría, exclusivamente propios para una institución. Incluye áreas, conteo esperado de áreas, estado revisado, aviso de cambios, autor, fecha y versión. Se conserva el endpoint anterior `/deliveries` por compatibilidad.
- `PUT /api/reviews/packages/{year}/review?establishmentId=UUID`: solo Secretaría; cuerpo `{ "version": "huella devuelta por la consulta" }`. Bloquea la autoevaluación mientras compara la versión y registra la revisión.
- `DELETE /api/reviews/packages/{year}/review?establishmentId=UUID`: solo Secretaría; devuelve el paquete pendiente actualizado.

La consulta de PMI se realiza en lote para las autoevaluaciones recibidas. Los cambios de Anexos 2–5 actualizan la fecha del PMI; la finalización de seguimiento también actualiza esa fecha para que su revisión previa no quede vigente ante nueva información institucional. Los comentarios de Secretaría no invalidan la marca.

## Verificación realizada

Suite completa tras incorporar paquetes: **152 pruebas sin fallos**, incluidas nueve en `DeliveryReviewTests` sobre roles y pertenencia, exclusión de borradores, conservación de autor y respuestas, comentarios inválidos, componentes ajenos, historial fuera de fechas y con catálogo desactivado, paso al siguiente año, reinicio del mismo año y lectura del PMI con rechazo de escritura por Secretaría. Siete pruebas adicionales en `DeliveryPackageTests` verifican agrupación por institución/año, PMI asociados, persistencia e idempotencia de revisión, nuevas áreas o cambios del PMI, independencia de comentarios, vuelta a pendiente, separación entre instituciones/años, reinicio del mismo año y permisos. Las pruebas usan PostgreSQL local con transacciones revertidas; V17 y V18 se aplicaron y el esquema pasó `ddl-auto=validate`.

El frontend pasó compilación/TypeScript y ESLint en los archivos modificados. El recorrido aislado de Chrome comprobó ambos roles, filtros por DANE, texto y año, evidencia textual/enlace, error y reintento de publicación sin perder el borrador, persistencia al recargar, texto de comentario escapado, consulta de los Anexos 2–5 y controles históricos, acceso al historial mientras el seguimiento bloquea el nuevo año, error explícito al consultar comentarios, rechazo de una institución ajena y diseño móvil de 390 px sin desbordamiento. Los datos de ese recorrido son simulados en el navegador y no modifican entregas reales.

El recorrido adicional de paquetes comprobó una tarjeta por institución/año con varias áreas, búsqueda por nombre y DANE, filtros de año/pendientes/revisados, error de guardado sin marcar optimistamente, revisión persistente al recargar, vuelta a pendiente, rechazo y actualización de una versión anterior, aviso de cambios posteriores, acceso al documento institucional correcto, paquetes propios sin acciones de escritura y vista móvil sin desbordamiento. Se conservaron los colores SEM de Antonio.
