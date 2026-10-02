# Desarrollo local y pruebas con Postman

## PostgreSQL

1. Crea una base de datos llamada `pmiautoevaluacion` en tu PostgreSQL local.
2. Crea `backend/src/main/resources/application-local.properties` con estas propiedades y tus valores locales:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/pmiautoevaluacion
   spring.datasource.username=postgres
   spring.datasource.password=<tu_contrasena_local>
   jwt.secret=<una_clave_larga_y_privada>
   ```

   Este archivo está excluido de Git. `application.properties` usa variables de entorno y debe permanecer sin credenciales.

3. Desde PowerShell, dentro de `backend`, inicia el servidor:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'local'
   .\gradlew.bat bootRun
   ```

Flyway crea las tablas al iniciar. El servidor escucha en `http://localhost:8080`.

## Postman

Necesitas un usuario de prueba en la base de datos: el proyecto todavía no expone un endpoint de registro. La contraseña almacenada debe ser un hash BCrypt.

| Acción | Método y URL | JSON o cabecera |
| --- | --- | --- |
| Entrar como Secretaría | `POST http://localhost:8080/api/auth/secretary/login` | `{"email":"<correo>","password":"<contraseña>"}` |
| Entrar como institución | `POST http://localhost:8080/api/auth/establishment/login` | `{"daneCode":"<código DANE>","password":"<contraseña>"}` |
| Consultar la sesión | `GET http://localhost:8080/api/me` | `Authorization: Bearer <token recibido>` |

Las rutas de login devuelven un `token`. `/api/me` requiere ese token; sin él responde `401`.

## Borrador anual

Con el token de una institución educativa, usa estas rutas:

| Acción | Método y URL | Cuerpo |
| --- | --- | --- |
| Crear o recuperar borrador | `PUT http://localhost:8080/api/self-evaluations/2026` | Sin cuerpo |
| Consultar borrador | `GET http://localhost:8080/api/self-evaluations/2026` | Sin cuerpo |
| Listar años propios | `GET http://localhost:8080/api/self-evaluations` | Sin cuerpo |
| Guardar valoración | `PUT http://localhost:8080/api/self-evaluations/2026/valuations/<componentId>` | `{"level":3,"evidenceUrl":"https://ejemplo.org/evidencia","evidenceNote":"Descripción de la evidencia"}` |

El nivel debe estar entre 1 y 4. La nota y el enlace son opcionales mientras el registro esté en borrador. El componente debe existir y estar activo en el catálogo. El catálogo aún necesita su carga inicial con datos oficiales; una lista vacía en la vista de autoevaluación es esperable hasta entonces.
