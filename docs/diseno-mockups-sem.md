# Adaptación visual de los mockups SEM

Solicitud de Juan del 28 de octubre de 2026: implementar el diseño de Antonio a partir de las 18 imágenes de `Practicas/evidecia y diagramas/SEM Mockups`. Se revisaron las pantallas de acceso, inicio, nueva/continuación de autoevaluación, formulario, envío, historial, búsqueda y revisión de Secretaría, fechas, cuenta y las láminas `SAMPLE`/`Home SAMPLE`.

El diseño usa la paleta ya definida en `base.css`: azul `#2B2C6B`, rojo `#E10818`, amarillo `#F7B032` y neutros del manual. Se mantiene Leelawadee UI y los pesos existentes. Los PNG son una referencia visual; las reglas institucionales siguen siendo las confirmadas por Juan y documentadas en `estado-proyecto.md`, no se sustituyen por textos de ejemplo de las imágenes.

## Correspondencia de pantallas

| Mockup | Interfaz adaptada | Conservación del comportamiento |
| --- | --- | --- |
| Inicio de sesión de Secretaría/institución | `/login`: dos paneles, selector de rol con iconos, formulario, identidad institucional y ondas | Usa los endpoints existentes de cada rol, con correo o DANE según corresponda; conserva validación y mensajes reales. |
| Home institución/Secretaría y Home SAMPLE | `/dashboard`: barra azul, menú lateral, tarjetas de autoevaluación/PMI y definición de fechas para Secretaría, menú de cuenta | Las tarjetas conducen a rutas reales; Secretaría consulta los paquetes. La integración de Antonio añade **Usuarios** para registrar cuentas. |
| Crear, continuar y no habilitado | `/dashboard?section=autoevaluacion`: cuatro áreas y aviso de período abierto o no habilitado | Los borradores se preparan desde Secretaría; se mantiene el requisito de seguimiento anterior y la selección del período publicado. |
| Formulario y tabla de creación | `/autoevaluacion/:areaId?year=...`: navegación entre cuatro áreas, procesos con encabezado azul y columnas de componente/valoración/evidencias/fortalezas/oportunidades en escritorio | Cada área mantiene su entrega independiente. Ayuda, autosave y formularios se conservan, con una sola referencia de evidencia y límites reales. En pantalla pequeña se disponen en tarjetas para poder leer y diligenciar. |
| Verificación de envío | Se conserva la sección **Entrega de esta área** y sus conteos/porcentajes | Guardar y enviar espera los cambios pendientes y comprueba completitud. No se cambia a un envío único anual ni se marcan otras áreas como enviadas. |
| Explorar autoevaluación y Find Autoevaluación | `/entregas`: búsqueda por nombre/área o DANE, filtros de año y revisión, paquetes institucionales con documentos por área | Se conserva la agrupación por institución/año solicitada posteriormente, la pertenencia institucional y la marca persistente de revisión. |
| Revisión y tabla de revisión | `/entregas/:year/areas/:areaId`: áreas recibidas del paquete, tabla de respuestas y observaciones por componente en escritorio | Respuestas de solo lectura; comentarios solo de Secretaría y solo en autoevaluación. La institución consulta las observaciones propias. El PMI sigue siendo de solo consulta. |
| Definición de fechas | `/secretaria/periodos-autoevaluacion`: año, inicio y fin organizados en un panel, confirmación y lista de períodos | Mantiene las operaciones existentes de publicación/edición/eliminación, sus confirmaciones y fechas de Bogotá. No se añade un plazo de PMI que no se ha definido. |
| Actualización de datos | `/mi-cuenta`: presentación y edición de los datos reales de la sesión | La integración de Antonio conecta `PATCH /api/me`, con contraseña opcional y cierre de sesión después de un guardado exitoso. |
| PMI y seguimiento | Anexos 2–5, a través del marco común | La lámina no trae un diseño específico de los anexos: se conserva su organización y se aplica el marco visual compartido. |

## Componentes compartidos y recursos

`AppShell` concentra navegación lateral, perfil y salida y monta una sola instancia de `AppTopBar`, incorporada desde Antonio. `SemIcon` reutiliza su biblioteca mediante `SvgIcon`; `SemWave` presenta `Curva-vector.svg`. `EvaluationAreaSteps` representa la navegación por área y `ReviewAreaLinks` permite moverse entre las áreas enviadas del paquete. `mockups.css` organiza el marco común sin cambiar la paleta de Antonio.

La primera adaptación mostró el escudo desde una copia sin modificar de `SAMPLE.png`. Tras integrar Antonio se utiliza directamente `Logo-SEM.svg` en `InstitutionalBrand`; el PNG anterior queda como referencia, sin cargarse en la interfaz. La zona que el mockup deja como `PLACEHOLDER` en acceso contiene el nombre del sistema y texto institucional, sin inventar una fotografía. El documento HTML declara idioma español y título del sistema.

## Reglas preservadas y diferencias de los bocetos

| Regla | Fuente | Verificación |
| --- | --- | --- |
| Un envío y un PMI por área | Aclaraciones previas de Juan | Cambiar de área conserva lo guardado y no envía las demás áreas. |
| Una evidencia que admite enlaces o texto | Solicitud de Juan del 26 de octubre | Se conserva el campo de 20.002 caracteres, la información anterior combinada y el guardado automático. No se aplica el ejemplo de 500 caracteres ni se restauran dos recuadros. |
| Ayuda con descripción y evidencias pertinentes | Respuesta de Juan y ayuda implementada | Se mantiene el diálogo y sus fuentes; no se añaden los cuatro descriptores de nivel solo porque aparezcan en la imagen. |
| Fortalezas/oportunidades dependen del estado manual | Reglas previas de Juan | No se automatiza la calificación ni se cambia “oportunidades de mejora” por una categoría nueva de “debilidades”. |
| Historial, comentarios y marca de revisión | Solicitudes de Juan del 27 de octubre | Paquetes, permisos, consulta de años anteriores y seguimiento previo siguen vigentes. |
| Salir respeta los cambios pendientes | Conservación del comportamiento de los formularios | La acción navega primero por `/salir`; solo tras superar las protecciones de navegación se cierra la sesión. Cancelar mantiene los datos y la sesión. |

No se incorporan controles de guardado simulados. La integración de Antonio habilita registro de usuarios para Secretaría y edición de la cuenta propia con sus API reales. La recuperación de contraseña por correo continúa pendiente; el acceso informa cómo solicitar ayuda. Véase [integración de Antonio](integracion-anton-2026-10-28.md).

## Validación

- Compilación Vue/TypeScript y ESLint de los componentes, vistas y rutas modificados.
- Recorridos aislados de Chrome con datos simulados: acceso de ambos roles, errores de acceso, menú/cuenta, Escape y retorno de foco, cuatro áreas, cambio de área, cuenta en consulta, fechas y estados habilitado/no habilitado.
- Comprobación de salida cancelada con cambios sin guardar: la sesión permanece abierta y el formulario conserva sus valores.
- Recorridos existentes de ayuda/evidencias, autosave, envío, requisito de seguimiento, Anexo 5, comentarios por componente, Anexos 2–5 de consulta, paquetes y marca de revisión.
- Escritorio de 1440 px y móvil de 390 px sin desbordamiento horizontal. En móvil, el menú es desplegable, el contenido de fondo queda inactivo mientras está abierto y Escape lo cierra.

La adaptación visual inicial no cambió servicios, esquema o permisos de backend. La integración posterior de Antonio incorpora sus servicios de usuarios y los ajustes documentados. Los recorridos visuales no escriben instituciones ni entregas reales en PostgreSQL. Las capturas y utilidades locales están en `.codex`, fuera del control de versiones.
