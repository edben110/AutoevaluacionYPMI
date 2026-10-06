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
La institución puede abrir `/autoevaluacion` desde su espacio, crear un borrador para un año y guardar valoraciones de componentes activos. El catálogo oficial todavía requiere una carga inicial en la base de datos.

## Verificación

```powershell
npm.cmd run build
npm.cmd exec eslint src -- --no-fix
```

La compilación incluye la comprobación de tipos. El inicio de sesión completo requiere backend, base de datos y usuarios de prueba configurados.
