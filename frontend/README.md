# Frontend de AutoevaluacionYPMI

Aplicación Vue 3, TypeScript y Vite. La primera pantalla conecta el inicio de sesión de instituciones educativas y Secretaría de Educación Municipal (SEM) con la API del proyecto.

## Desarrollo local

Requiere Node.js compatible con `package.json`. En PowerShell, usar `npm.cmd`:

```powershell
npm.cmd ci
npm.cmd run dev
```

El servidor de Vite envía las peticiones `/api` a `http://localhost:8080`; inicia el backend por separado en ese puerto. Si el backend no está disponible, el formulario mostrará un error de conexión.

## Contrato usado

- Institución: `POST /api/auth/establishment/login` con `daneCode` y `password`.
- SEM: `POST /api/auth/secretary/login` con `email` y `password`.
- Perfil: `GET /api/me` con el token recibido en `Authorization: Bearer ...`.

El token se conserva en `sessionStorage` durante la pestaña actual y se elimina al cerrar sesión o si el servidor rechaza la sesión. La pantalla `/dashboard` muestra el perfil y el acceso al PMI institucional.
El espacio de la institución muestra cuatro entradas de autoevaluación, una por área. Cada una abre `/autoevaluacion/:areaId` con sus propios procesos, componentes y avance; el registro anual y las respuestas existentes se conservan. La entrada anterior `/autoevaluacion` vuelve al espacio para elegir el área. La migración V6 del backend carga el catálogo de 2026: 4 áreas, 15 procesos y 45 componentes. Consulta [el inventario y su fuente](../docs/catalogo-guia34-2026.md).

Cada componente permite guardar evidencias y los textos que admite su estado: Existencia solo oportunidades; Pertinencia y Apropiación ambos textos; Mejoramiento continuo solo fortalezas. Los campos permitidos son opcionales en borrador. Si al cambiar el estado queda un texto incompatible, se conserva visible y puede quitarse explícitamente antes de guardar. Las tablas presentan conteos y porcentajes de los procesos y del área seleccionada sobre valoraciones guardadas, con encabezados de procesos y tablas en azul SEM. El backend actualizado aplica V7 para almacenar los dos textos; reinícialo antes de probar la pantalla.

**Ayuda del componente** abre dentro de la pantalla la descripción de qué evalúa el ítem y ejemplos orientadores de evidencia, con fuente y páginas de la Guía 34. La ayuda también está disponible en áreas enviadas. **Evidencia: enlace o descripción** admite hasta 10.000 caracteres, texto de documentos físicos, enlaces y saltos de línea con guardado automático; no interpreta ese texto como HTML ni exige escaneos. Requiere V13 y el recurso de ayuda del backend. Véase [alcance y pertinencia de evidencias](../docs/ayuda-componentes-evidencias.md).

## Colores compartidos

La paleta de Antonio se mantiene en `src/assets/base.css`. El acceso, el panel institucional, las autoevaluaciones, las tablas de conteos, Secretaría y el PMI usan esas variables: azul SEM para acciones y encabezados, fondos blancos/neutros, rojo para errores o eliminación y amarillo para foco/avisos. El 23 de octubre se sustituyeron los verdes provisionales restantes, incluidos fondos, bordes y estados de interacción. El icono de la pestaña utiliza los mismos colores. La revisión visual usa datos aislados y la compilación incluye Vue/TypeScript.

## Verificación

```powershell
npm.cmd run build
npm.cmd exec eslint src -- --no-fix
```

La compilación incluye la comprobación de tipos. El inicio de sesión completo requiere backend, base de datos y usuarios de prueba configurados.
## Períodos de autoevaluación

Secretaría dispone de `/secretaria/periodos-autoevaluacion` desde **Administrar períodos** en su espacio. Configura año, inicio y fin; el backend prepara automáticamente los borradores de las instituciones registradas. En el espacio institucional las cuatro áreas y sus formularios aparecen cuando el período está abierto. Reiniciar el backend después de incorporar V8 para disponer de los endpoints. Véase [documentación de períodos](../docs/periodos-autoevaluacion.md).

**Eliminar período** solicita confirmación y retira la habilitación anual. Conserva las autoevaluaciones y respuestas; configurar nuevamente el año permite continuar con sus borradores existentes.

La institución ve las cuatro áreas únicamente durante un período abierto por Secretaría. Se retiraron el año libre y el botón de creación manual. Si no hay un período abierto, aparece un aviso de espera; al regresar a la pestaña o actualizar la disponibilidad se consulta de nuevo al servidor. Los enlaces directos verifican el período antes de cargar respuestas o componentes. Si hay varios períodos abiertos, solo se pueden elegir esos años.

## PMI: priorización del Anexo 2

La institución encuentra **Planes de mejoramiento por área**, con tarjetas por año y progreso de cada área. `/pmi/:year` resume las áreas; `/pmi/:year/areas/:areaId` abre el PMI habilitado tras valorar todos los componentes activos y enviar la autoevaluación de esa área. El Anexo 2 toma solo el menor estado presente (Existencia, en su ausencia Pertinencia y después Apropiación); si todos están en Mejoramiento continuo no hay candidatos. Después de guardar factores y U/T/I de todos los candidatos, pasan los máximos: uno solo, dos automáticamente, o dos elegidos entre tres o más empatados. Nunca se completa el cupo con menor puntaje. Los formularios conservan las ediciones pendientes de otras filas al guardar una.

La pantalla puede usarse después del cierre de autoevaluación, utiliza los colores y la tipografía actuales de Antonio y requiere el backend actualizado con V9. Véase [trazabilidad y API](../docs/pmi-anexo2.md).

## PMI: formulación del Anexo 3

Desde la priorización se abre `/pmi/:year/areas/:areaId/formulacion` cuando la autoevaluación está completa y enviada y se guardaron los U/T/I de todos los candidatos. Allí se puede confirmar la selección; `canFormulate` sigue siendo necesario para guardar los objetivos de los componentes elegidos. Cada objetivo admite varias metas y filas de indicadores/actividades, con costo/FSE/otra fuente, responsable, fechas e instrumentos y frecuencia de recolección. Se admiten borradores incompletos y se distinguen importes pendientes de cero. Los totales corresponden a objetivos activos guardados del área.

La entrada también permite resolver la selección cuando ya se puntuaron todos los candidatos y hay más de dos máximos empatados. La pantalla simplificada del 23 de octubre presenta **Paso 1: elegir componentes** mediante tarjetas y **Generar formularios**; **Paso 2: completar el plan de cada componente**. El botón prepara los campos iniciales de objetivo, meta, indicador, actividades, responsable, fechas, recursos y seguimiento sin duplicar lo ya escrito. Solo **Guardar este plan** persiste el objetivo y sus respuestas. Las tarjetas respetan el máximo de dos componentes y el caso de un único máximo. El resumen de recursos y la opción de otro objetivo quedan como consultas secundarias para facilitar el inicio.

Cada objetivo se guarda por separado y las ediciones pendientes de los demás se conservan. Las eliminaciones requieren confirmación; cambiar la selección de componentes conserva los objetivos anteriores para consulta. Los montos se transportan como cadenas decimales para evitar redondeos. Los estilos locales usan la paleta y tipografía SEM existentes. Reiniciar el backend para aplicar V10 y V11; esta última separa los planes por área conservando los datos anteriores. Véase [contrato y fuente del Anexo 3](../docs/pmi-anexo3.md).

## Guardado automático y envío de cada área

Las respuestas de autoevaluación se guardan automáticamente tras una pausa de unos 800 ms. Cada componente muestra cambios pendientes, guardando, guardado o error y permite **Guardar ahora/Reintentar**. Antes de enviar se guardan y verifican todas las ediciones pendientes; un fallo impide la entrega.

**Enviar autoevaluación de esta área** confirma la entrega únicamente de esa área, deja sus valoraciones para consulta y habilita su propio PMI. Las otras áreas conservan su borrador. La API usa GET `/api/self-evaluations/{year}/areas/{areaId}` y POST de la misma ruta con `/submit`. Reiniciar el backend para aplicar V12: los datos anteriores siguen sin enviar hasta que la institución lo confirme. Véase [contrato y reglas](../docs/envio-autoevaluacion-por-area.md).
