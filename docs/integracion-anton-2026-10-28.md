# Integración de los avances de Antonio en JuanM

Juan autorizó incorporar los cambios después de revisar su contenido y simular la combinación. Rama de origen: `origin/anton`, hasta `bf70ef9`; base de `JuanM` antes de integrar: `683e09c`. Se conserva el historial de ambas ramas mediante un merge, sin modificar `anton` ni `main`.

## Cambios incorporados

| Commit de Antonio | Aporte |
| --- | --- |
| `3059c84` | Registro de instituciones y usuarios de Secretaría desde SEM; actualización de la cuenta autenticada mediante `PATCH /api/me`; DTO, servicios y validaciones de duplicados. |
| `f75f3d0` | Rediseño del acceso; logo SEM, ondas y biblioteca de iconos SVG. |
| `4800de5` | Rediseño del panel y estilos reutilizables. |
| `bf70ef9` | Componentes `AppTopBar` y `SvgIcon` para barra y menús compartidos. |

Los conflictos de `LoginView.vue` y `DashboardView.vue` se resolvieron combinando los recursos/componentes de Antonio con las rutas y funciones de JuanM. La barra se monta una sola vez desde `AppShell` y sus botones controlan la navegación/cuenta existentes. Se conservan el menú móvil, Escape, retorno de foco y los avisos de formularios pendientes. Los iconos heredan el color y respetan `fill="none"`; la onda utiliza el SVG de Antonio ajustado a su contenedor. El logo se muestra directamente desde `Logo-SEM.svg`, sustituyendo el recorte visual del PNG anterior.

El panel conserva las cuatro autoevaluaciones por área, el seguimiento obligatorio previo, los paquetes, las observaciones y la consulta del PMI. Los estilos globales y utilitarios incorporados conviven con el marco de `mockups.css`, sin cambiar la paleta ni la tipografía del equipo.

## Funciones disponibles en la interfaz

- **Secretaría → Usuarios**: registrar una institución educativa o una cuenta de Secretaría con los endpoints de Antonio. La pantalla muestra el resultado real del servidor; no envía correos ni expone la contraseña en la confirmación. Una institución no tiene acceso a esa pantalla ni a las API de registro.
- **Datos de la cuenta → Editar datos**: nombre, correo y nueva contraseña opcional; DANE y rector únicamente para instituciones. La petición PATCH contiene solo los campos modificados. Los errores conservan el formulario; el guardado exitoso cierra la sesión actual y vuelve al acceso con el rol seleccionado y un mensaje para ingresar nuevamente.
- **Cancelar / salir con datos pendientes**: cancelar la edición restaura los datos consultados. Salir hacia otra pantalla requiere confirmar el descarte si hay cambios; cancelar conserva la sesión y el formulario.

La recuperación de contraseña por correo continúa pendiente. El cierre de sesión de la interfaz no representa un nuevo mecanismo de revocación global de tokens.

## Ajustes necesarios para combinar ambos desarrollos

| Regla y origen | Datos / validación | Aceptación |
| --- | --- | --- |
| La SEM administra el registro; permisos implementados por Antonio | `POST /api/users/establishments` y `/secretaries`, rol `SECRETARY` | 201 con datos públicos; institución recibe 403 y acceso anónimo 401. |
| La cuenta se actualiza desde la identidad autenticada; API de Antonio | `PATCH /api/me`, sin selector de otro usuario | Cambian solo sus datos, se mantienen los campos omitidos y no se pierden sus autoevaluaciones. |
| Instituciones registradas durante un período abierto deben poder diligenciarlo; compatibilidad con el flujo de JuanM | Creación transaccional de la institución y sus borradores para períodos abiertos según el reloj de Bogotá | Borradores vacíos vinculados al período; sin valoraciones, envíos, PMI ni formularios de períodos futuros o cerrados. |
| Los datos deben respetar las columnas existentes | Límites de nombre/correo/rector 150, DANE 20 y contraseña de entrada 255, según los DTO de cuenta | La validación de registro rechaza campos que excedan el esquema antes de intentar persistirlos. No se modifica el esquema. |
| Evitar escribir desde una instancia antigua del usuario autenticado | El servicio recarga al usuario por su ID dentro de la transacción | Los PATCH preservan los campos no enviados y la identidad de la institución y sus relaciones. |

La preparación de borradores se agrega a `EstablishmentServiceImpl.create`; no reinicia respuestas de instituciones existentes ni reconfigura fechas. La actualización conserva el cifrado de contraseñas de Antonio y las validaciones de correo y DANE duplicados. No se agrega ninguna migración ni se cambian las reglas del State, U/T/I o Anexos 2–5.

## Verificación

- **160 pruebas de backend sin fallos**, incluidas ocho nuevas en `UserManagementIntegrationTests`: registro por rol, contraseñas codificadas, datos públicos de respuesta, duplicados y límites, borradores de un período abierto, actualización propia/parcial, acceso posterior con DANE/contraseña nuevos y conservación de la autoevaluación.
- Frontend: compilación Vue/TypeScript y ESLint de los archivos de integración.
- Navegador aislado: registro de ambos tipos de usuario, errores y reintento, acceso institucional rechazado, cuenta de ambos roles, PATCH de campos modificados, cierre de sesión exitoso, cancelación de navegación con cambios, una sola barra, recursos SVG y móvil de 390 px.
- Regresión visual de los flujos de autoevaluación, ayuda/evidencia única, autosave y envío, seguimiento previo, paquetes y marca de revisión, comentarios por componente y consulta de Anexos 2–5.

Las pruebas de persistencia son transaccionales y se revierten; las pruebas del navegador usan datos simulados. No se cambian las credenciales ni los datos de las cuentas locales existentes.
