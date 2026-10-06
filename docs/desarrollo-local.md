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

`level` identifica el estado elegido manualmente: `1` = Existencia, `2` = Pertinencia, `3` = Apropiación y `4` = Mejoramiento continuo. La nota y el enlace son opcionales mientras el registro esté en borrador. El componente debe existir y estar activo en el catálogo. El catálogo aún necesita su carga inicial con datos oficiales; una lista vacía en la vista de autoevaluación es esperable hasta entonces.

La respuesta de una valoración conserva `level` y añade `state` (por ejemplo, `APPROPRIATION`) y `stateLabel` (`Apropiación`). La consulta o creación del borrador devuelve además `totals`, con el número de valoraciones guardadas en `existence`, `pertinence`, `appropriation`, `continuousImprovement` y su `total`. Un componente aporta una unidad a su estado, sin multiplicar por el código 1–4. El borrador sin valoraciones devuelve todos los conteos en cero.

La vista muestra los estados por nombre y calcula subtotales por proceso y totales por área a partir del catálogo visible y las valoraciones guardadas. El total del borrador incluye todas sus valoraciones, también las de componentes desactivados posteriormente. Seleccionar otro estado sin guardar todavía no modifica los conteos. No se requiere una migración adicional: los registros numéricos existentes se reconstruyen como objetos State al consultarlos.

Consulta [valoración de componentes](valoracion-componentes.md) para el diseño State y la relación con la futura priorización del PMI.
