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

El token se conserva en `sessionStorage` durante la pestaña actual y se elimina al cerrar sesión o si el servidor rechaza la sesión. La pantalla `/dashboard` muestra el perfil. La formulación del PMI se integrará después.
El espacio de la institución muestra cuatro entradas de autoevaluación, una por área. Cada una abre `/autoevaluacion/:areaId` con sus propios procesos, componentes y avance; el registro anual y las respuestas existentes se conservan. La entrada anterior `/autoevaluacion` vuelve al espacio para elegir el área. La migración V6 del backend carga el catálogo de 2026: 4 áreas, 15 procesos y 45 componentes. Consulta [el inventario y su fuente](../docs/catalogo-guia34-2026.md).

Cada componente permite guardar evidencias y los textos que admite su estado: Existencia solo oportunidades; Pertinencia y Apropiación ambos textos; Mejoramiento continuo solo fortalezas. Los campos permitidos son opcionales en borrador. Si al cambiar el estado queda un texto incompatible, se conserva visible y puede quitarse explícitamente antes de guardar. Las tablas presentan conteos y porcentajes de los procesos y del área seleccionada sobre valoraciones guardadas, con estilos modestos y encabezados de procesos verde oscuro. El backend actualizado aplica V7 para almacenar los dos textos; reinícialo antes de probar la pantalla.

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
